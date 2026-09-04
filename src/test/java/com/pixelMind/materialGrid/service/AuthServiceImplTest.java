package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.LoginRequest;
import com.pixelMind.materialGrid.dto.response.LoginResponse;
import com.pixelMind.materialGrid.entity.User;
import com.pixelMind.materialGrid.entity.enums.Role;
import com.pixelMind.materialGrid.entity.enums.UserStatus;
import com.pixelMind.materialGrid.exception.UnauthorizedException;
import com.pixelMind.materialGrid.mapper.UserMapper;
import com.pixelMind.materialGrid.repository.UserRepository;
import com.pixelMind.materialGrid.security.AuthenticationService;
import com.pixelMind.materialGrid.security.SecurityUserDetails;
import com.pixelMind.materialGrid.security.jwt.JwtTokenProvider;
import com.pixelMind.materialGrid.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login_success_generatesJwtToken() {
        User user = User.builder()
                .id(1L)
                .username("shehan")
                .status(UserStatus.ACTIVE)
                .role(Role.ROLE_ADMIN)
                .password("hashed")
                .build();
        SecurityUserDetails principal = new SecurityUserDetails(user);

        when(authenticationService.authenticate("shehan", "Passw0rd1")).thenReturn(principal);
        when(jwtTokenProvider.generateToken("shehan", 1L, Role.ROLE_ADMIN)).thenReturn("mock-jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        LoginResponse response = authService.login(new LoginRequest("shehan", "Passw0rd1"));

        assertThat(response.getAccessToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getSessionToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUsername()).isEqualTo("shehan");
        assertThat(response.getRole()).isEqualTo("ROLE_ADMIN");
        assertThat(response.getExpiresIn()).isEqualTo(86400000L);
    }

    @Test
    void login_invalidCredentials_throwsUnauthorized() {
        when(authenticationService.authenticate("shehan", "wrong"))
                .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("shehan", "wrong")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logout_clearsContextSuccessfully() {
        authService.logout("some-token");
        // successfully completes without throwing exception
    }
}
