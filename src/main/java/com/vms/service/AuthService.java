package com.vms.service;

import com.vms.dto.AuthResponse;
import com.vms.dto.LoginRequest;
import com.vms.dto.UserResponse;
import com.vms.entity.User;
import com.vms.repository.UserRepository;
import com.vms.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing user authentication, registration, and profile retrieval.
 * Employs BCrypt for password hashing and enforces strict secret isolation.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates user credentials and generates a signed JWT token.
     *
     * @param request the login request containing username and password
     * @return AuthResponse containing token and user metadata
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting authentication for user: '{}'", request.getUsername());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            log.warn("Authentication failed for user: '{}' - Invalid credentials", request.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenProvider.generateToken(authentication);
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        log.info("User '{}' authenticated successfully. Assigned roles: {}", request.getUsername(), roles);

        return new AuthResponse(
                token,
                "Bearer",
                jwtTokenProvider.getExpirationInSeconds(),
                authentication.getName(),
                roles
        );
    }

    /**
     * Registers a new user with BCrypt hashed password.
     *
     * @param username the desired username
     * @param password the plain text password to be hashed
     * @param email    optional email address
     * @param role     assigned role (defaults to ROLE_USER)
     * @return UserResponse containing public user metadata
     */
    @Transactional
    public UserResponse register(String username, String password, String email, String role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }

        String assignedRole = (role != null && !role.isBlank()) ? role : "ROLE_USER";
        if (!assignedRole.startsWith("ROLE_")) {
            assignedRole = "ROLE_" + assignedRole;
        }

        // Securely hash password with BCrypt
        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(username, encodedPassword, email, assignedRole);
        User saved = userRepository.save(user);

        log.info("User registered successfully: '{}' with role '{}'", username, assignedRole);

        return new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRole());
    }

    /**
     * Retrieves the profile of the currently authenticated user from SecurityContext.
     *
     * @return UserResponse metadata
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BadCredentialsException("No authenticated user found in security context.");
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Authenticated user not found in database: " + username));

        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
