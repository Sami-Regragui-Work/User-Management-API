package org.spring.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.dto.request.ChangePasswordRequest;
import org.spring.dto.request.UserCreateRequest;
import org.spring.dto.request.UserUpdateRequest;
import org.spring.exception.DuplicateEmailException;
import org.spring.exception.ForbiddenException;
import org.spring.exception.InvalidPasswordException;
import org.spring.exception.UserNotFoundException;
import org.spring.model.User;
import org.spring.model.enums.UserRole;
import org.spring.repository.UserRepository;
import org.spring.security.AuthenticatedUser;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock private PasswordEncoder encoder;
    @InjectMocks
    private UserService userService;

    private final AuthenticatedUser admin = new AuthenticatedUser(1L, UserRole.ADMIN);
    private final AuthenticatedUser normal = new AuthenticatedUser(2L, UserRole.USER);
    private final UserCreateRequest createDto = new UserCreateRequest("N", "n@x.com", "password123", UserRole.USER);
    private final UserUpdateRequest updateDto = new UserUpdateRequest("New", "taken@x.com");

    @Test
    void createUser_asUser_isForbidden() {
        assertThrows(ForbiddenException.class, () -> userService.createUser(normal, createDto));
        verifyNoInteractions(userRepository);
    }

    @Test
    void createUser_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("n@x.com")).thenReturn(true);
        assertThrows(DuplicateEmailException.class, () -> userService.createUser(admin, createDto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_storesHashedPassword() {
        when(encoder.encode("password123")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.createUser(admin, createDto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("HASH", captor.getValue().getHashedPassword());
    }

    @Test
    void updateUser_unknownId_is404_beforeEmailCheck() {
        when(userRepository.findByIdAndActiveTrue(9L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.updateUser(admin, 9L, updateDto));
        verify(userRepository, never()).existsByEmailAndIdNot(any(), any());
    }

    @Test
    void updateUser_emailTakenByAnother_throwsConflict() {
        when(userRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(new User("A", "a@x.com", UserRole.USER)));
        when(userRepository.existsByEmailAndIdNot("taken@x.com", 5L)).thenReturn(true);
        assertThrows(DuplicateEmailException.class, () -> userService.updateUser(admin, 5L, updateDto));
    }

    @Test
    void deactivateUser_setsInactive() {
        User target = new User("A", "a@x.com", UserRole.USER);
        when(userRepository.findByIdAndActiveTrue(5L)).thenReturn(Optional.of(target));
        userService.deactivateUser(admin, 5L);
        assertFalse(target.isActive());
    }

    @Test
    void changePassword_wrongCurrent_throwsAndDoesNotEncode() {
        User me = new User("M", "m@x.com", UserRole.USER);
        me.setHashedPassword("OLD");
        when(userRepository.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(me));
        when(encoder.matches("bad", "OLD")).thenReturn(false);

        assertThrows(InvalidPasswordException.class,
                () -> userService.changePassword(normal, new ChangePasswordRequest("bad", "newPassword1")));
        verify(encoder, never()).encode(any());
    }

    @Test
    void changePassword_ok_replacesHash() {
        User me = new User("M", "m@x.com", UserRole.USER);
        me.setHashedPassword("OLD");
        when(userRepository.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(me));
        when(encoder.matches("old", "OLD")).thenReturn(true);
        when(encoder.encode("newPassword1")).thenReturn("NEW");

        userService.changePassword(normal, new ChangePasswordRequest("old", "newPassword1"));
        assertEquals("NEW", me.getHashedPassword());
    }
}