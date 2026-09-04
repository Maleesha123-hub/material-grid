package com.pixelMind.materialGrid.dto.response;

import com.pixelMind.materialGrid.entity.enums.Role;
import com.pixelMind.materialGrid.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private UserStatus status;
    private Role role;
    private String createdBy;
    private LocalDateTime createdDate;
    private String modifiedBy;
    private LocalDateTime modifiedDate;
    // Intentionally no password field - never expose credentials via API.
}
