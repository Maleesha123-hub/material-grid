package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.entity.UserSession;

public interface SessionService {

    /**
     * Atomically supersedes any existing ACTIVE session for the user and
     * creates a new one. See implementation for the concurrency guarantee.
     */
    UserSession createSession(Long userId);

    void invalidateSession(String sessionToken);

    UserSession validateAndTouch(String sessionToken);
}
