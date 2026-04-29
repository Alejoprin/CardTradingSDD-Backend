package com.cardtrading.inventory.entity;

import com.cardtrading.auth.entity.User;
import com.cardtrading.card.entity.Card;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_cards", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "card_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "acquired_from", nullable = false, length = 20)
    private AcquisitionSource acquiredFrom;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (acquiredAt == null) {
            acquiredAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AcquisitionSource {
        SYSTEM, TRADE, PURCHASE, MANUAL
    }
}
