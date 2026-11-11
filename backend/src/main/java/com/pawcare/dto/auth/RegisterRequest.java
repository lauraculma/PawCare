package com.pawcare.dto.auth;

import com.pawcare.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String fullName,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 160, message = "El correo no puede superar 160 caracteres")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 100, message = "La contrasena debe tener entre 8 y 100 caracteres")
        String password,

        @Size(max = 30, message = "El telefono no puede superar 30 caracteres")
        String phone,

        Role role
) {
}
