package org.spring.security;

import org.spring.model.enums.UserRole;

public record AuthenticatedUser(Long id, UserRole role) {}
