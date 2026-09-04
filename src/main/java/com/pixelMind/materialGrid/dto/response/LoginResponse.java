package com.pixelMind.materialGrid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String sessionToken; // Alias for backward compatibility
    private String tokenType;    // "Bearer"
    private Long expiresIn;      // Token lifetime in milliseconds
    private String username;
    private String role;
}
