package com.cardtrading.card.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_id", nullable = false)
    private CardSet set;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "card_number", length = 50)
    private String cardNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "card_rarity")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private Rarity rarity;

    @Column(columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String attributes;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "image_small_url", length = 500)
    private String imageSmallUrl;

    @Column(name = "market_price", precision = 10, scale = 2)
    private BigDecimal marketPrice;

    @Column(name = "last_price_update")
    private LocalDateTime lastPriceUpdate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Rarity {
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, SECRET,
        BASE, RAINBOW, GLITTER, CRYSTAL_SHINE, GOLDEN_GLITTER
    }

}
