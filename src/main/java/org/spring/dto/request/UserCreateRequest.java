package org.spring.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.spring.model.enums.UserRole;

public record UserCreateRequest(
        @NotBlank(message = "Name field cannot be blank")
        String name,
        @Email(message = "Wrong email format")
        @NotBlank(message = "Email field cannot be blank")
        String email,
        @Size(min = 8, max = 72, message = "Password field must be at least 8 characters and must not surpass 72")
        @NotBlank(message = "Password field cannot be blank")
        String password,
        @NotNull(message = "Role is required")
        UserRole role
) {}
