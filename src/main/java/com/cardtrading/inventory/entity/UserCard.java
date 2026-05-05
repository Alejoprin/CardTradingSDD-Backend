package com.cardtrading.inventory.entity;

import com.cardtrading.auth.entity.User;
import com.cardtrading.card.entity.Card;
import com.cardtrading.card.entity.CustomCard;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_cards")
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
    @JoinColumn(name = "card_id")
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_card_id")
    private CustomCard customCard;

    @Column(nullable = false)
    @Builder.Default
    private int quantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "card_condition")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private CardCondition condition = CardCondition.NEAR_MINT;

    @Column(name = "is_for_trade", nullable = false)
    @Builder.Default
    private boolean forTrade = false;

    @Column(name = "is_for_sale", nullable = false)
    @Builder.Default
    private boolean forSale = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

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

    public enum CardCondition {
        MINT, NEAR_MINT, EXCELLENT, GOOD, PLAYED, POOR
    }
}
