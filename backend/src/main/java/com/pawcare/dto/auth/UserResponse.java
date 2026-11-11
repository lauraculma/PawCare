package com.pawcare.dto.auth;

import com.pawcare.entity.Role;
import com.pawcare.entity.User;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        String phone,
        boolean approved
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone(),
                user.isApproved()
        );
    }
}
