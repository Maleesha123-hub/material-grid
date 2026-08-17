package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.request.UserCreateRequest;
import com.pixelMind.materialGrid.dto.request.UserUpdateRequest;
import com.pixelMind.materialGrid.dto.response.UserResponse;
import com.pixelMind.materialGrid.entity.User;
import com.pixelMind.materialGrid.entity.enums.UserStatus;
import com.pixelMind.materialGrid.exception.DuplicateResourceException;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.mapper.UserMapper;
import com.pixelMind.materialGrid.repository.UserRepository;
import com.pixelMind.materialGrid.service.UserService;
import com.pixelMind.materialGrid.util.SecurityUtil;
import com.pixelMind.materialGrid.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException(
                    "Username already exists: " + request.getUsername(),
                    ErrorCodeConstants.DUPLICATE_USERNAME);
        }

        String actor = SecurityUtil.getCurrentUsername();

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .createdBy(actor)
                .modifiedBy(actor)
                .build();

        User saved = userRepository.save(user);
        log.info("User created: username={}, by={}", saved.getUsername(), actor);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return userMapper.toResponse(findUserOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = findUserOrThrow(id);

        if (!ValidationUtil.isBlank(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        user.setModifiedBy(SecurityUtil.getCurrentUsername());

        User saved = userRepository.save(user);
        log.info("User updated: id={}, by={}", saved.getId(), user.getModifiedBy());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = findUserOrThrow(id);
        userRepository.delete(user);
        log.info("User deleted: id={}, by={}", id, SecurityUtil.getCurrentUsername());
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id, ErrorCodeConstants.USER_NOT_FOUND));
    }
}
