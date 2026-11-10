package com.pawcare.dto.auth;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMinutes,
        UserResponse user
) {
    public static AuthResponse of(String token, long expiresInMinutes, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInMinutes, user);
    }
}
