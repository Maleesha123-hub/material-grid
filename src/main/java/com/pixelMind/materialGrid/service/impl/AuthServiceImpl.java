//package com.pixelMind.materialGrid.service.impl;
//
//import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
//import com.pixelMind.materialGrid.dto.request.LoginRequest;
//import com.pixelMind.materialGrid.dto.response.LoginResponse;
//import com.pixelMind.materialGrid.dto.response.UserResponse;
//import com.pixelMind.materialGrid.entity.User;
//import com.pixelMind.materialGrid.entity.UserSession;
//import com.pixelMind.materialGrid.exception.UnauthorizedException;
//import com.pixelMind.materialGrid.mapper.UserMapper;
//import com.pixelMind.materialGrid.repository.UserRepository;
//import com.pixelMind.materialGrid.security.AuthenticationService;
//import com.pixelMind.materialGrid.security.SecurityUserDetails;
//import com.pixelMind.materialGrid.service.AuthService;
//import com.pixelMind.materialGrid.service.SessionService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class AuthServiceImpl implements AuthService {
//
//    private final AuthenticationService authenticationService;
//    private final SessionService sessionService;
//    private final UserRepository userRepository;
//    private final UserMapper userMapper;
//
//    @Override
//    public LoginResponse login(LoginRequest request) {
//        log.info("Login attempt for username={}", request.getUsername());
//
//        SecurityUserDetails principal;
//        try {
//            principal = authenticationService.authenticate(request.getUsername(), request.getPassword());
//        } catch (BadCredentialsException e) {
//            log.warn("Failed login attempt for username={}", request.getUsername());
//            throw new UnauthorizedException("Invalid username or password", ErrorCodeConstants.INVALID_CREDENTIALS);
//        }
//
//        UserSession session = sessionService.createSession(principal.getUserId());
//        log.info("Successful login for username={}", request.getUsername());
//
//        return LoginResponse.builder()
//                .sessionToken(session.getSessionToken())
//                .username(principal.getUsername())
//                .tokenType("Bearer")
//                .build();
//    }
//
//    @Override
//    @Transactional
//    public void logout(String sessionToken) {
//        sessionService.invalidateSession(sessionToken);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public UserResponse getCurrentUser(String sessionToken) {
//        UserSession session = sessionService.validateAndTouch(sessionToken);
//        User user = userRepository.findById(session.getUserId())
//                .orElseThrow(() -> new UnauthorizedException(
//                        "Authenticated user no longer exists", ErrorCodeConstants.UNAUTHORIZED));
//        return userMapper.toResponse(user);
//    }
//}
