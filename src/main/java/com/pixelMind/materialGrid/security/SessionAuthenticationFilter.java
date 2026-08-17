package com.pixelMind.materialGrid.security;

import com.pixelMind.materialGrid.constant.SecurityConstants;
import com.pixelMind.materialGrid.entity.User;
import com.pixelMind.materialGrid.entity.UserSession;
import com.pixelMind.materialGrid.entity.enums.SessionStatus;
import com.pixelMind.materialGrid.repository.UserRepository;
import com.pixelMind.materialGrid.repository.UserSessionRepository;
import com.pixelMind.materialGrid.util.DateTimeUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Runs on every request. This is what makes session invalidation *real*
 * rather than a cosmetic DB flag: every protected request re-validates the
 * presented token against the database, and only an ACTIVE row with an
 * unexpired idle window results in an authenticated SecurityContext.
 * A session that has been superseded by a newer login (flipped to
 * LOGGED_OUT) or has gone idle too long is rejected here, immediately -
 * not "eventually" or "next time a flag is checked".
 */
@Slf4j
@RequiredArgsConstructor
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final SecurityContextService securityContextService;
    private final long sessionIdleTimeoutMinutes;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            Optional<UserSession> sessionOpt =
                    userSessionRepository.findBySessionTokenAndStatus(token, SessionStatus.ACTIVE);

            if (sessionOpt.isPresent()) {
                UserSession session = sessionOpt.get();

                if (isIdleExpired(session)) {
                    session.setStatus(SessionStatus.EXPIRED);
                    session.setLogoutDate(DateTimeUtil.nowUtc());
                    userSessionRepository.save(session);
                    log.info("Session expired due to inactivity for userId={}", session.getUserId());
                } else {
                    Optional<User> userOpt = userRepository.findById(session.getUserId());
                    if (userOpt.isPresent()) {
                        session.setLastAccessDate(DateTimeUtil.nowUtc());
                        userSessionRepository.save(session);
                        securityContextService.setAuthenticatedUser(userOpt.get());
                    }
                }
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            securityContextService.clear();
        }
    }

    private boolean isIdleExpired(UserSession session) {
        LocalDateTime cutoff = session.getLastAccessDate().plusMinutes(sessionIdleTimeoutMinutes);
        return DateTimeUtil.nowUtc().isAfter(cutoff);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTH_HEADER);
        if (header != null && header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return header.substring(SecurityConstants.TOKEN_PREFIX.length()).trim();
        }
        return null;
    }
}
