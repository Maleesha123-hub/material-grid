package com.pixelMind.materialGrid.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around Spring Security's AuthenticationManager. Kept separate
 * from AuthService (business/session orchestration) so credential
 * verification stays a single-responsibility concern that is easy to unit
 * test and swap (e.g. for LDAP) independently of session logic.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;

    public SecurityUserDetails authenticate(String username, String rawPassword) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, rawPassword)
            );
            return (SecurityUserDetails) authentication.getPrincipal();
        } catch (BadCredentialsException | org.springframework.security.authentication.DisabledException e) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }
}
