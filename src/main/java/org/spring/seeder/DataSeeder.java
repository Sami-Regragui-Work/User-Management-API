package org.spring.seeder;

import org.spring.model.User;
import org.spring.model.enums.UserRole;
import org.spring.repository.UserRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements InitializingBean {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Override
    public void afterPropertiesSet() {
        if (this.userRepository.count() > 0) return;

        this.userRepository.saveAll(List.of(
                this.build("Admin", "admin@example.com", "admin@example.com", UserRole.ADMIN, true),
                this.build("User1", "user1@example.com", "user1@example.com", UserRole.USER, true),
                this.build("User2", "user2@example.com", "user2@example.com", UserRole.USER, true),
                this.build("DeactivatedUser", "d_user@example.com", "d_user@example.com", UserRole.USER, false)
        ));
    }

    private User build(String name, String email, String rawPassword, UserRole role, boolean active) {
        User user = new User(name, email, role);
        user.setHashedPassword(this.encoder.encode(rawPassword));
        user.setActive(active);
        return user;
    }
}
