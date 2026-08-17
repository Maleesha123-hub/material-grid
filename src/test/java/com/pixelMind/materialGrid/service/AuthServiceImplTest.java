package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.LoginRequest;
import com.pixelMind.materialGrid.dto.response.LoginResponse;
import com.pixelMind.materialGrid.entity.User;
import com.pixelMind.materialGrid.entity.UserSession;
import com.pixelMind.materialGrid.entity.enums.SessionStatus;
import com.pixelMind.materialGrid.entity.enums.UserStatus;
import com.pixelMind.materialGrid.exception.UnauthorizedException;
import com.pixelMind.materialGrid.mapper.UserMapper;
import com.pixelMind.materialGrid.repository.UserRepository;
import com.pixelMind.materialGrid.security.AuthenticationService;
import com.pixelMind.materialGrid.security.SecurityUserDetails;
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
    private SessionService sessionService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login_success_createsSession() {
        User user = User.builder().id(1L).username("shehan").status(UserStatus.ACTIVE).password("hashed").build();
        SecurityUserDetails principal = new SecurityUserDetails(user);
        UserSession session = UserSession.builder()
                .id(1L).userId(1L).sessionToken("new-token").status(SessionStatus.ACTIVE).build();

        when(authenticationService.authenticate("shehan", "Passw0rd1")).thenReturn(principal);
        when(sessionService.createSession(1L)).thenReturn(session);

        LoginResponse response = authService.login(new LoginRequest("shehan", "Passw0rd1"));

        assertThat(response.getSessionToken()).isEqualTo("new-token");
        assertThat(response.getUsername()).isEqualTo("shehan");
    }

    @Test
    void login_invalidCredentials_throwsUnauthorized() {
        when(authenticationService.authenticate("shehan", "wrong"))
                .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("shehan", "wrong")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logout_delegatesToSessionService() {
        authService.logout("some-token");
        org.mockito.Mockito.verify(sessionService).invalidateSession("some-token");
    }
}
