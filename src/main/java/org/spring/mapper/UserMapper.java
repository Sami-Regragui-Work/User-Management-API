package org.spring.mapper;

import org.spring.dto.request.UserCreateRequest;
import org.spring.dto.response.UserResponse;
import org.spring.dto.request.UserUpdateRequest;
import org.spring.model.User;

public class UserMapper {
    public static User toEntity(UserCreateRequest dto) {
        return new User(dto.name(), dto.email(), dto.role());
    }

    public static void updateEntity(UserUpdateRequest dto, User existing) {
        existing.setName(dto.name());
        existing.setEmail(dto.email());
    }

    public static UserResponse toResponse(User entity) {
        return new UserResponse(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getRole(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

}
