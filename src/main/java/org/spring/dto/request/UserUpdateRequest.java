package org.spring.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
        @NotBlank(message = "Name field cannot be blank")
        String name,
        @Email(message = "Wrong email format")
        @NotBlank(message = "Email field cannot be blank")
        String email) {}
