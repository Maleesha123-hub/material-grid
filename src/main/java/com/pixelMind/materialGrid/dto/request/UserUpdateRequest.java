package com.pixelMind.materialGrid.dto.request;

import com.pixelMind.materialGrid.entity.enums.Role;
import com.pixelMind.materialGrid.entity.enums.UserStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    // Password change is optional on update; null/blank means "leave unchanged".
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Password must contain at least one uppercase letter, one lowercase letter and one digit"
    )
    private String password;

    private UserStatus status;

    private Role role;

    public UserUpdateRequest(String password, UserStatus status) {
        this.password = password;
        this.status = status;
    }
}
