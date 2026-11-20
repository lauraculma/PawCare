package com.pawcare.dto.pet;

import com.pawcare.entity.User;

public record OwnerSummary(
        String id,
        String fullName,
        String email,
        String cedula
) {
    public static OwnerSummary from(User user) {
        String publicId = user.getCedula() == null ? String.valueOf(user.getId()) : user.getCedula();
        return new OwnerSummary(publicId, user.getFullName(), user.getEmail(), user.getCedula());
    }
}
