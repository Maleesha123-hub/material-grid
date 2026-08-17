package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.UserSession;
import com.pixelMind.materialGrid.entity.enums.SessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionTokenAndStatus(String sessionToken, SessionStatus status);

    Optional<UserSession> findByUserIdAndStatus(Long userId, SessionStatus status);

    /**
     * Pessimistic write lock on the current active session row (if any) for a
     * user. Used to serialize concurrent login attempts for the same user so
     * that "invalidate old + create new" is atomic and a second, simultaneous
     * login request cannot slip in between the check and the write.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserSession s where s.userId = :userId and s.status = 'ACTIVE'")
    Optional<UserSession> findActiveByUserIdForUpdate(@Param("userId") Long userId);
}
