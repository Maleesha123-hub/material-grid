package com.pixelMind.materialGrid.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * Base64-encoded secret key or plain text string of at least 256 bits (32 bytes) for HMAC-SHA256.
     */
    private String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    /**
     * Expiration duration in milliseconds (default 24 hours: 86400000ms).
     */
    private long expirationMs = 86_400_000L;

    /**
     * HTTP header carrying the JWT token (default: Authorization).
     */
    private String header = "Authorization";

    /**
     * Token prefix in header (default: "Bearer ").
     */
    private String prefix = "Bearer ";
}
