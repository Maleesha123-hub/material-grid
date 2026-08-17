package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.UserCreateRequest;
import com.pixelMind.materialGrid.dto.request.UserUpdateRequest;
import com.pixelMind.materialGrid.dto.response.UserResponse;
import com.pixelMind.materialGrid.entity.User;
import com.pixelMind.materialGrid.entity.enums.UserStatus;
import com.pixelMind.materialGrid.exception.DuplicateResourceException;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.mapper.UserMapper;
import com.pixelMind.materialGrid.repository.UserRepository;
import com.pixelMind.materialGrid.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(1L)
                .username("shehan")
                .password("hashed")
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void createUser_success() {
        UserCreateRequest request = new UserCreateRequest("newuser", "Passw0rd1");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd1")).thenReturn("hashed-pw");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });
        when(userMapper.toResponse(any(User.class))).thenReturn(
                UserResponse.builder().id(2L).username("newuser").status(UserStatus.ACTIVE).build());

        UserResponse response = userService.createUser(request);

        assertThat(response.getUsername()).isEqualTo("newuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_duplicateUsername_throws() {
        UserCreateRequest request = new UserCreateRequest("shehan", "Passw0rd1");
        when(userRepository.existsByUsername("shehan")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void getUser_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateUser_changesStatusAndPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("NewPassw0rd1")).thenReturn("new-hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(
                UserResponse.builder().id(1L).username("shehan").status(UserStatus.INACTIVE).build());

        UserUpdateRequest request = new UserUpdateRequest("NewPassw0rd1", UserStatus.INACTIVE);
        UserResponse response = userService.updateUser(1L, request);

        assertThat(response.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(existingUser.getPassword()).isEqualTo("new-hashed");
    }

    @Test
    void deleteUser_notFound_throws() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
