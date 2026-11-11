package com.pawcare.dto.auth;

public record RegistrationResponse(
        boolean requiresApproval,
        String token,
        String tokenType,
        long expiresInMinutes,
        UserResponse user
) {
    public static RegistrationResponse pending(UserResponse user) {
        return new RegistrationResponse(true, null, null, 0, user);
    }

    public static RegistrationResponse authenticated(AuthResponse auth) {
        return new RegistrationResponse(false, auth.token(), auth.tokenType(), auth.expiresInMinutes(), auth.user());
    }
}
