package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.UserCreateRequest;
import com.pixelMind.materialGrid.dto.request.UserUpdateRequest;
import com.pixelMind.materialGrid.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    UserResponse getUser(Long id);

    Page<UserResponse> getUsers(Pageable pageable);

    UserResponse updateUser(Long id, UserUpdateRequest request);

    void deleteUser(Long id);
}
