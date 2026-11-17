package com.vms.security;

import com.vms.dto.LoginRequest;
import com.vms.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderSecurityTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("BCrypt generates unique salted hashes for the same password")
    void bcrypt_generatesUniqueSaltedHashes() {
        String rawPassword = "superSecretPassword123!";

        String hash1 = passwordEncoder.encode(rawPassword);
        String hash2 = passwordEncoder.encode(rawPassword);

        assertThat(hash1).isNotEqualTo(rawPassword);
        assertThat(hash2).isNotEqualTo(rawPassword);
        // Salted output: two encodings of the same plain text must differ
        assertThat(hash1).isNotEqualTo(hash2);

        // Both verify correctly against the raw password
        assertThat(passwordEncoder.matches(rawPassword, hash1)).isTrue();
        assertThat(passwordEncoder.matches(rawPassword, hash2)).isTrue();
        assertThat(passwordEncoder.matches("wrongPassword", hash1)).isFalse();
    }

    @Test
    @DisplayName("User entity toString excludes password to prevent log leaks")
    void userToString_neverContainsPassword() {
        User user = new User(1L, "admin", "$2a$10$encryptedPasswordHash", "admin@vms.corp", "ROLE_ADMIN", true);

        String representation = user.toString();

        assertThat(representation).contains("admin");
        assertThat(representation).contains("ROLE_ADMIN");
        assertThat(representation).doesNotContain("$2a$10$encryptedPasswordHash");
        assertThat(representation).doesNotContain("password");
    }

    @Test
    @DisplayName("LoginRequest toString masks plain text password to prevent log leaks")
    void loginRequestToString_masksPassword() {
        LoginRequest request = new LoginRequest("admin", "plainTextPassword123");

        String representation = request.toString();

        assertThat(representation).contains("admin");
        assertThat(representation).doesNotContain("plainTextPassword123");
        assertThat(representation).contains("[PROTECTED]");
    }
}
