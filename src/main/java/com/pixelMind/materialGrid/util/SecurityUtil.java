package com.pixelMind.materialGrid.util;

import com.pixelMind.materialGrid.security.SecurityUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.SecureRandom;
import java.util.Base64;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Returns the username of the currently authenticated principal, as
     * resolved by Spring Security from the validated server-side session -
     * never trusted from client-supplied request fields.
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUserDetails userDetails) {
            return userDetails.getUsername();
        }
        return authentication.getName();
    }

    public static String generateSessionToken(int lengthBytes) {
        byte[] randomBytes = new byte[lengthBytes];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
