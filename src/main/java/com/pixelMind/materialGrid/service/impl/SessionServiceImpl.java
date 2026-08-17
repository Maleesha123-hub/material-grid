package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.constant.SecurityConstants;
import com.pixelMind.materialGrid.entity.UserSession;
import com.pixelMind.materialGrid.entity.enums.SessionStatus;
import com.pixelMind.materialGrid.exception.UnauthorizedException;
import com.pixelMind.materialGrid.repository.UserSessionRepository;
import com.pixelMind.materialGrid.service.SessionService;
import com.pixelMind.materialGrid.util.DateTimeUtil;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implements the "single active login" rule (see class-level design note in
 * SecurityConfig / SessionAuthenticationFilter for the full rationale).
 *
 * Concurrency strategy: {@code createSession} takes a PESSIMISTIC_WRITE lock
 * on the user's current ACTIVE session row (if one exists) via
 * {@link UserSessionRepository#findActiveByUserIdForUpdate}, all inside a
 * single REQUIRES_NEW transaction. If two login requests for the same user
 * arrive simultaneously, the second blocks at the database lock until the
 * first transaction commits (superseding the old session and inserting the
 * new one); it then re-reads a state where the first request's session is
 * already the active one, supersedes *that*, and proceeds. This serializes
 * the two logins rather than letting them race, and the DB-level unique
 * constraint on the generated "active_marker" column (see
 * V2__create_user_sessions_table.sql) is the final backstop even if the
 * locking were ever bypassed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final UserSessionRepository userSessionRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserSession createSession(Long userId) {
        Optional<UserSession> existingActive = userSessionRepository.findActiveByUserIdForUpdate(userId);

        existingActive.ifPresent(old -> {
            old.setStatus(SessionStatus.LOGGED_OUT);
            old.setLogoutDate(DateTimeUtil.nowUtc());
            userSessionRepository.save(old);
            log.info("Superseded previous active session for userId={} (session replacement)", userId);
        });

        UserSession newSession = UserSession.builder()
                .userId(userId)
                .sessionToken(SecurityUtil.generateSessionToken(SecurityConstants.SESSION_TOKEN_LENGTH_BYTES))
                .status(SessionStatus.ACTIVE)
                .build();

        UserSession saved = userSessionRepository.save(newSession);
        log.info("New active session created for userId={}", userId);
        return saved;
    }

    @Override
    @Transactional
    public void invalidateSession(String sessionToken) {
        UserSession session = userSessionRepository
                .findBySessionTokenAndStatus(sessionToken, SessionStatus.ACTIVE)
                .orElseThrow(() -> new UnauthorizedException(
                        "No active session found", ErrorCodeConstants.SESSION_INVALID));

        session.setStatus(SessionStatus.LOGGED_OUT);
        session.setLogoutDate(DateTimeUtil.nowUtc());
        userSessionRepository.save(session);
        log.info("Session logged out for userId={}", session.getUserId());
    }

    @Override
    @Transactional
    public UserSession validateAndTouch(String sessionToken) {
        UserSession session = userSessionRepository
                .findBySessionTokenAndStatus(sessionToken, SessionStatus.ACTIVE)
                .orElseThrow(() -> new UnauthorizedException(
                        "Session is invalid, expired, or has been superseded",
                        ErrorCodeConstants.SESSION_INVALID));
        session.setLastAccessDate(DateTimeUtil.nowUtc());
        return userSessionRepository.save(session);
    }
}
