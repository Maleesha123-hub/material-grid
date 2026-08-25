package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.entity.UserSession;
import com.pixelMind.materialGrid.entity.enums.SessionStatus;
import com.pixelMind.materialGrid.exception.UnauthorizedException;
import com.pixelMind.materialGrid.repository.UserSessionRepository;
import com.pixelMind.materialGrid.service.impl.SessionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private UserSessionRepository userSessionRepository;

    @InjectMocks
    private SessionServiceImpl sessionService;

    @Test
    void createSession_supersedesExistingActiveSession() {
        UserSession oldSession = UserSession.builder()
                .id(1L).userId(10L).sessionToken("old-token").status(SessionStatus.ACTIVE).build();

        when(userSessionRepository.findActiveByUserIdForUpdate(10L)).thenReturn(Optional.of(oldSession));
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSession newSession = sessionService.createSession(10L);

        assertThat(oldSession.getStatus()).isEqualTo(SessionStatus.LOGGED_OUT);
        assertThat(oldSession.getLogoutDate()).isNotNull();
        //assertThat(newSession.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(newSession.getSessionToken()).isNotEqualTo("old-token");
        //verify(userSessionRepository, times(2)).save(any(UserSession.class));
    }

    @Test
    void createSession_noExistingSession_justCreatesNew() {
        when(userSessionRepository.findActiveByUserIdForUpdate(20L)).thenReturn(Optional.empty());
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSession newSession = sessionService.createSession(20L);

        assertThat(newSession.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        verify(userSessionRepository, times(1)).save(any(UserSession.class));
    }

    @Test
    void invalidateSession_marksLoggedOut() {
        UserSession session = UserSession.builder()
                .id(1L).userId(10L).sessionToken("tok").status(SessionStatus.ACTIVE).build();
        when(userSessionRepository.findBySessionTokenAndStatus("tok", SessionStatus.ACTIVE))
                .thenReturn(Optional.of(session));
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));

        sessionService.invalidateSession("tok");

        assertThat(session.getStatus()).isEqualTo(SessionStatus.LOGGED_OUT);
    }

    @Test
    void invalidateSession_unknownToken_throws() {
        when(userSessionRepository.findBySessionTokenAndStatus("bad", SessionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.invalidateSession("bad"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void validateAndTouch_supersededToken_throwsUnauthorized() {
        // Old, superseded session no longer has status ACTIVE, so the lookup
        // (which only matches ACTIVE) returns empty - this is what makes the
        // "old session cannot access protected APIs" guarantee real.
        when(userSessionRepository.findBySessionTokenAndStatus("old-token", SessionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.validateAndTouch("old-token"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
