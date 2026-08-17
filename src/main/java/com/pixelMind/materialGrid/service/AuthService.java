package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.LoginRequest;
import com.pixelMind.materialGrid.dto.response.LoginResponse;
import com.pixelMind.materialGrid.dto.response.UserResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout(String sessionToken);

    UserResponse getCurrentUser(String sessionToken);
}
