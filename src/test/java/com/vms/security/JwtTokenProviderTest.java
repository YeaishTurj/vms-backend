package com.vms.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String TEST_SECRET = "vms-unit-test-secret-key-must-be-at-least-256-bits-long-2026";
    private static final long TEST_EXPIRATION_MS = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", TEST_EXPIRATION_MS);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("generateToken generates valid signed JWT and parses username correctly")
    void generateToken_shouldProduceValidToken() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        String token = jwtTokenProvider.generateToken(auth);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo("admin");
        assertThat(jwtTokenProvider.getRolesFromToken(token)).containsExactly("ROLE_ADMIN");
        assertThat(jwtTokenProvider.getExpirationInSeconds()).isEqualTo(3600);
    }

    @Test
    @DisplayName("generateTokenFromUsername produces valid token with multiple roles")
    void generateTokenFromUsername_shouldProduceValidToken() {
        List<String> roles = List.of("ROLE_ADMIN", "ROLE_USER");
        String token = jwtTokenProvider.generateTokenFromUsername("superadmin", roles);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo("superadmin");
        assertThat(jwtTokenProvider.getRolesFromToken(token)).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("validateToken rejects malformed, invalid or empty tokens")
    void validateToken_shouldRejectInvalidTokens() {
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
        assertThat(jwtTokenProvider.validateToken("   ")).isFalse();
        assertThat(jwtTokenProvider.validateToken("invalid.token.structure")).isFalse();
        assertThat(jwtTokenProvider.validateToken("header.payload.invalidsignature")).isFalse();
    }

    @Test
    @DisplayName("validateToken rejects expired token")
    void validateToken_shouldRejectExpiredToken() {
        // Initialize provider with negative expiration to simulate expired token
        JwtTokenProvider expiredProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(expiredProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(expiredProvider, "jwtExpirationMs", -1000L);
        expiredProvider.init();

        Authentication auth = new UsernamePasswordAuthenticationToken("user", null, Collections.emptyList());
        String expiredToken = expiredProvider.generateToken(auth);

        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }
}
