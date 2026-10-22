package org.spring.service;

import org.spring.dto.request.ChangePasswordRequest;
import org.spring.dto.request.UserCreateRequest;
import org.spring.dto.response.UserResponse;
import org.spring.dto.request.UserUpdateRequest;
import org.spring.exception.DuplicateEmailException;
import org.spring.exception.ForbiddenException;
import org.spring.exception.InvalidPasswordException;
import org.spring.exception.UserNotFoundException;
import org.spring.mapper.UserMapper;
import org.spring.model.User;
import org.spring.model.enums.UserRole;
import org.spring.repository.UserRepository;
import org.spring.security.AuthenticatedUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public UserService(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsersWithOptionalSizeAndPage(AuthenticatedUser caller, int size, int page) {
        this.requireAdmin(caller);

        Sort sort = Sort.by("id");
        Pageable pageable = (size < 1) ? Pageable.unpaged(sort) : PageRequest.of(Math.max(page, 0), size, sort);

        List<User> userEntities = this.userRepository.findAllByActiveTrue(pageable);
        return userEntities.stream().map(UserMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(AuthenticatedUser caller, Long id) {
        this.requireAdmin(caller);
        return UserMapper.toResponse(this.findByIdOrThrow(id));
    }

    public UserResponse createUser(AuthenticatedUser caller, UserCreateRequest dto) {
        this.requireAdmin(caller);
        if (this.userRepository.existsByEmail(dto.email())) {
            throw new DuplicateEmailException();
        }

        User user = UserMapper.toEntity(dto);
        user.setHashedPassword(this.encoder.encode(dto.password()));

        return UserMapper.toResponse(this.userRepository.save(user));
    }

    public UserResponse updateUser(AuthenticatedUser caller, Long id, UserUpdateRequest dto) {
        this.requireAdmin(caller);
        return this.applyUpdate(id, dto);
    }

    public void deactivateUser(AuthenticatedUser caller, Long id) {
        this.requireAdmin(caller);
        this.findByIdOrThrow(id).setActive(false);
    }

    private User findByIdOrThrow(Long id) {
        return this.userRepository.findByIdAndActiveTrue(id).orElseThrow(UserNotFoundException::new);
    }

    private void requireAdmin(AuthenticatedUser caller) {
        if (caller.role() != UserRole.ADMIN) throw new ForbiddenException();
    }

    private UserResponse applyUpdate(Long id, UserUpdateRequest dto) {
        User user = this.findByIdOrThrow(id);
        if (this.userRepository.existsByEmailAndIdNot(dto.email(), id))
            throw new DuplicateEmailException();

        UserMapper.updateEntity(dto, user);
        return UserMapper.toResponse(this.userRepository.saveAndFlush(user));
    }

    // USER Role methods
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(AuthenticatedUser caller) {
        return UserMapper.toResponse(this.findByIdOrThrow(caller.id()));
    }

    public UserResponse updateCurrentUser(AuthenticatedUser caller, UserUpdateRequest dto) {
        return this.applyUpdate(caller.id(), dto);
    }

    public void changePassword(AuthenticatedUser caller, ChangePasswordRequest dto) {
        User user = this.findByIdOrThrow(caller.id());
        if (!this.encoder.matches(dto.currentPassword(), user.getHashedPassword()))
            throw new InvalidPasswordException();
        user.setHashedPassword(this.encoder.encode(dto.newPassword()));
    }
}
