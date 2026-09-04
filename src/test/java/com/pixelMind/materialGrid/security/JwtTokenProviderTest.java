package com.pixelMind.materialGrid.security;

import com.pixelMind.materialGrid.config.JwtProperties;
import com.pixelMind.materialGrid.entity.enums.Role;
import com.pixelMind.materialGrid.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        jwtProperties.setExpirationMs(3600000L); // 1 hour
        jwtProperties.setHeader("Authorization");
        jwtProperties.setPrefix("Bearer ");

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    void generateToken_andValidate_success() {
        String token = jwtTokenProvider.generateToken("john_doe", 100L, Role.ROLE_ADMIN);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo("john_doe");
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(100L);
        assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("invalid.jwt.token")).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        // Create a provider with negative expiration to simulate expired token
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        expiredProps.setExpirationMs(-1000L);

        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProps);
        String expiredToken = expiredProvider.generateToken("expired_user", 1L, Role.ROLE_USER);

        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }
}
