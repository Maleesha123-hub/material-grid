/*
package com.pixelMind.materialGrid.integration;

import com.pixelMind.materialGrid.dto.request.LoginRequest;
import com.pixelMind.materialGrid.dto.request.UserCreateRequest;
import com.pixelMind.materialGrid.dto.response.LoginResponse;
import com.pixelMind.materialGrid.service.AuthService;
import com.pixelMind.materialGrid.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

*/
/**
 * Verifies the end-to-end single-active-session guarantee: a second login
 * with the same credentials supersedes the first, and the first session
 * token stops being valid immediately - not eventually.
 *//*

class SessionReplacementIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;

    @Test
    @WithMockUser(username = "system")
    void secondLogin_invalidatesFirstSessionImmediately() {
        userService.createUser(new UserCreateRequest("racer", "Passw0rd1"));
        SecurityContextHolder.clearContext();

        LoginResponse first = authService.login(new LoginRequest("racer", "Passw0rd1"));
        LoginResponse second = authService.login(new LoginRequest("racer", "Passw0rd1"));

        assertThat(first.getSessionToken()).isNotEqualTo(second.getSessionToken());

        // The old token must no longer resolve to an authenticated user.
        assertThatThrownBy(() -> authService.getCurrentUser(first.getSessionToken()))
                .isInstanceOf(com.pixelMind.materialGrid.exception.UnauthorizedException.class);

        // The new token must work.
        assertThat(authService.getCurrentUser(second.getSessionToken()).getUsername()).isEqualTo("racer");
    }
}
*/
