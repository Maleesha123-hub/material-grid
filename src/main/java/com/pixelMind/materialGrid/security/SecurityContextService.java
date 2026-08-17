package com.pixelMind.materialGrid.security;

import com.pixelMind.materialGrid.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Populates the Spring Security context for the current request thread once
 * a session token has been validated against the database. Kept as an
 * explicit, named step (rather than inlined in the filter) so the "how do we
 * turn a validated DB row into an authenticated principal" concern is
 * independently testable.
 */
@Component
public class SecurityContextService {

    public void setAuthenticatedUser(User user) {
        SecurityUserDetails userDetails = new SecurityUserDetails(user);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);
    }

    public void clear() {
        SecurityContextHolder.clearContext();
    }
}
