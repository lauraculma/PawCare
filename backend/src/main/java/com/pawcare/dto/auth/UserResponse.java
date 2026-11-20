package com.pawcare.dto.auth;

import com.pawcare.entity.Role;
import com.pawcare.entity.User;

public record UserResponse(
        String id,
        Long internalId,
        String fullName,
        String email,
        Role role,
        String phone,
        String cedula,
        boolean approved
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getCedula() == null ? String.valueOf(user.getId()) : user.getCedula(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone(),
                user.getCedula(),
                user.isApproved()
        );
    }
}
