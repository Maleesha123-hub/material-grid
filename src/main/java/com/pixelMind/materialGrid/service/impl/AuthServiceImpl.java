package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.request.LoginRequest;
import com.pixelMind.materialGrid.dto.response.LoginResponse;
import com.pixelMind.materialGrid.dto.response.UserResponse;
import com.pixelMind.materialGrid.entity.User;
import com.pixelMind.materialGrid.exception.UnauthorizedException;
import com.pixelMind.materialGrid.mapper.UserMapper;
import com.pixelMind.materialGrid.repository.UserRepository;
import com.pixelMind.materialGrid.security.AuthenticationService;
import com.pixelMind.materialGrid.security.SecurityUserDetails;
import com.pixelMind.materialGrid.security.jwt.JwtTokenProvider;
import com.pixelMind.materialGrid.service.AuthService;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationService authenticationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for username={}", request.getUsername());

        SecurityUserDetails principal;
        try {
            principal = authenticationService.authenticate(request.getUsername(), request.getPassword());
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for username={}", request.getUsername());
            throw new UnauthorizedException("Invalid username or password", ErrorCodeConstants.INVALID_CREDENTIALS);
        }

        String jwtToken = jwtTokenProvider.generateToken(
                principal.getUsername(),
                principal.getUserId(),
                principal.getRole()
        );

        log.info("Successful login for username={}, role={}", principal.getUsername(), principal.getRole());

        return LoginResponse.builder()
                .accessToken(jwtToken)
                .sessionToken(jwtToken) // backward compatibility
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .username(principal.getUsername())
                .role(principal.getRole() != null ? principal.getRole().name() : null)
                .build();
    }

    @Override
    public void logout(String token) {
        log.info("User logout requested: actor={}", SecurityUtil.getCurrentUsername());
        SecurityContextHolder.clearContext();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        String username = SecurityUtil.getCurrentUsername();
        if ("anonymous".equalsIgnoreCase(username)) {
            throw new UnauthorizedException("User is not authenticated", ErrorCodeConstants.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException(
                        "Authenticated user no longer exists", ErrorCodeConstants.UNAUTHORIZED));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String token) {
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UnauthorizedException(
                            "Authenticated user no longer exists", ErrorCodeConstants.UNAUTHORIZED));
            return userMapper.toResponse(user);
        }
        return getCurrentUser();
    }
}
