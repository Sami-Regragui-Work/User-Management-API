package org.spring.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.spring.exception.UnauthorizedException;
import org.spring.model.User;
import org.spring.repository.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public static final String AUTH_ATTRIBUTE = "authenticatedUser";

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public AuthInterceptor(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, @NonNull HttpServletResponse resp, @NonNull Object handler) throws Exception {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Basic ")) throw new UnauthorizedException();

        String credentials;
        try {
            byte[] bytes = Base64.getDecoder().decode(header.substring(6));
            credentials = new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException();
        }
        int splitter = credentials.indexOf(":");
        if (splitter == -1) throw new UnauthorizedException();
        String email = credentials.substring(0, splitter);
        String password = credentials.substring(splitter + 1);

        User user = this.userRepository.findByEmailAndActiveTrue(email).orElseThrow(UnauthorizedException::new);
        if (!encoder.matches(password, user.getHashedPassword())) throw new UnauthorizedException();

        req.setAttribute(AUTH_ATTRIBUTE, new AuthenticatedUser(user.getId(), user.getRole()));

        return true;
    }
}
