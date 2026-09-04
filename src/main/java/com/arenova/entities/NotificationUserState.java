package com.arenova.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "notification_user_states",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "notification_key"})
)
public class NotificationUserState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "notification_key", nullable = false, length = 80)
    private String notificationKey;

    @Column(nullable = false)
    @Builder.Default
    private boolean unread = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean favorite = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean archived = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touchUpdatedAt() {
        updatedAt = LocalDateTime.now();
    }
}
