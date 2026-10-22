package org.spring.controller;

import jakarta.validation.Valid;
import org.spring.dto.request.ChangePasswordRequest;
import org.spring.dto.request.UserCreateRequest;
import org.spring.dto.request.UserUpdateRequest;
import org.spring.dto.response.UserResponse;
import org.spring.security.AuthInterceptor;
import org.spring.security.AuthenticatedUser;
import org.spring.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(
            @RequestAttribute(AuthInterceptor.AUTH_ATTRIBUTE)
            AuthenticatedUser caller,
            @Valid @RequestBody
            UserCreateRequest dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.createUser(caller, dto));
    }

    @GetMapping
    public List<UserResponse> getAll(
            @RequestAttribute(AuthInterceptor.AUTH_ATTRIBUTE)
            AuthenticatedUser caller,
            @RequestParam(defaultValue = "0")
            int size,
            @RequestParam(defaultValue = "-1")
            int page
    ) {
        return this.userService.getAllUsersWithOptionalSizeAndPage(caller, size, page);
    }

    @GetMapping("/{id}")
    public UserResponse get(
            @RequestAttribute(AuthInterceptor.AUTH_ATTRIBUTE)
            AuthenticatedUser caller,
            @PathVariable Long id
    ) {
        return this.userService.getUserById(caller, id);
    }

    @PutMapping("/{id}")
    public UserResponse put(
            @RequestAttribute(AuthInterceptor.AUTH_ATTRIBUTE)
            AuthenticatedUser caller,
            @PathVariable Long id,
            @Valid @RequestBody
            UserUpdateRequest dto
    ) {
        return this.userService.updateUser(caller, id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @RequestAttribute(AuthInterceptor.AUTH_ATTRIBUTE)
            AuthenticatedUser caller,
            @PathVariable Long id
    ) {
        this.userService.deactivateUser(caller, id);
        return ResponseEntity.noContent().build(); //noContent returns a builder with status NO_CONTENT ==> 204
    }

    @GetMapping("/me")
    public UserResponse getCurrent(
            @RequestAttribute(AuthInterceptor.AUTH_ATTRIBUTE)
            AuthenticatedUser caller
    ) {
        return this.userService.getCurrentUser(caller);
    }

    @PutMapping("/me")
    public UserResponse putCurrent(
            @RequestAttribute(AuthInterceptor.AUTH_ATTRIBUTE)
            AuthenticatedUser caller,
            @Valid @RequestBody
            UserUpdateRequest dto
    ) {
        return this.userService.updateCurrentUser(caller, dto);
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @RequestAttribute(AuthInterceptor.AUTH_ATTRIBUTE)
            AuthenticatedUser caller,
            @Valid @RequestBody
            ChangePasswordRequest dto
    ) {
        this.userService.changePassword(caller, dto);
        return ResponseEntity.noContent().build();
    }

}
