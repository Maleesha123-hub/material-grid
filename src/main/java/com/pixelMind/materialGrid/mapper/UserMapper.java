package com.pixelMind.materialGrid.mapper;

import com.pixelMind.materialGrid.dto.response.UserResponse;
import com.pixelMind.materialGrid.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .status(user.getStatus())
                .role(user.getRole())
                .createdBy(user.getCreatedBy())
                .createdDate(user.getCreatedDate())
                .modifiedBy(user.getModifiedBy())
                .modifiedDate(user.getModifiedDate())
                .build();
    }
}
