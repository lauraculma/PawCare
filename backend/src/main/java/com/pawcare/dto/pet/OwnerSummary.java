package com.pawcare.dto.pet;

import com.pawcare.entity.User;

public record OwnerSummary(
        Long id,
        String fullName,
        String email
) {
    public static OwnerSummary from(User user) {
        return new OwnerSummary(user.getId(), user.getFullName(), user.getEmail());
    }
}
