package com.pixelMind.materialGrid.controller;

import com.pixelMind.materialGrid.constant.SecurityConstants;
import com.pixelMind.materialGrid.dto.request.LoginRequest;
import com.pixelMind.materialGrid.dto.response.ApiResponse;
import com.pixelMind.materialGrid.dto.response.LoginResponse;
import com.pixelMind.materialGrid.dto.response.UserResponse;
import com.pixelMind.materialGrid.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Authentication and session management")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login. Any existing active session for this user is invalidated.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @Operation(summary = "Logout the current session")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        authService.logout(extractToken(request));
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @Operation(summary = "Get the currently authenticated user")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(HttpServletRequest request) {
        UserResponse response = authService.getCurrentUser(extractToken(request));
        return ResponseEntity.ok(ApiResponse.success("Current user retrieved", response));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTH_HEADER);
        if (header != null && header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return header.substring(SecurityConstants.TOKEN_PREFIX.length()).trim();
        }
        return null;
    }
}
