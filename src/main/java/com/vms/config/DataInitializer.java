package com.vms.config;

import com.vms.entity.User;
import com.vms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes default system user accounts on first application startup if the database is empty.
 * Passwords are securely hashed with BCrypt prior to persistence.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("No users found in database. Initializing default system accounts with BCrypt password hashing...");

            // Default Administrator account
            User admin = new User(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "admin@vms.corp",
                    "ROLE_ADMIN"
            );
            userRepository.save(admin);

            // Default Standard User account
            User user = new User(
                    "user",
                    passwordEncoder.encode("user123"),
                    "user@vms.corp",
                    "ROLE_USER"
            );
            userRepository.save(user);

            log.info("Default user accounts successfully initialized: 'admin' (ROLE_ADMIN), 'user' (ROLE_USER).");
        }
    }
}
