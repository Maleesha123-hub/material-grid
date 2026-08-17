package com.pixelMind.materialGrid.entity;

import com.pixelMind.materialGrid.entity.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_token", nullable = false, unique = true, length = 100)
    private String sessionToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "login_date", nullable = false, updatable = false)
    private LocalDateTime loginDate;

    @Column(name = "last_access_date", nullable = false)
    private LocalDateTime lastAccessDate;

    @Column(name = "logout_date")
    private LocalDateTime logoutDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.loginDate = now;
        this.lastAccessDate = now;
        if (this.status == null) {
            this.status = SessionStatus.ACTIVE;
        }
    }
}
