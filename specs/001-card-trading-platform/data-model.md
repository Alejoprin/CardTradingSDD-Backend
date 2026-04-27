# Data Model: Card Trading Platform

**Phase**: 1 — Design
**Date**: 2026-04-23
**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

---

## Entities

### User

**Table**: `users`
**Soft delete**: `deleted_at` (constitution mandate)

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | UUID | PK, NOT NULL | Auto-generated |
| `username` | VARCHAR(20) | UNIQUE, NOT NULL | 3–20 characters |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | Valid email format |
| `password` | VARCHAR(255) | NOT NULL | BCrypt hash |
| `role` | VARCHAR(20) | NOT NULL, DEFAULT 'USER' | Enum: `USER`, `ADMIN` |
| `is_banned` | BOOLEAN | NOT NULL, DEFAULT false | Admin ban flag |
| `created_at` | TIMESTAMP | NOT NULL | Auto-set on insert |
| `updated_at` | TIMESTAMP | NOT NULL | Auto-set on update |
| `deleted_at` | TIMESTAMP | NULLABLE | Soft-delete timestamp |

**Indexes**:
- `idx_users_email` on `email`
- `idx_users_username` on `username`
- `idx_users_deleted_at` on `deleted_at` (partial filter for active queries)

**Validation rules**:
- `username`: 3–20 characters, alphanumeric + underscore
- `email`: RFC 5322 valid format
- `password` (plain text input): min 8 chars, ≥1 uppercase, ≥1 lowercase, ≥1 digit
- `role`: must be one of `USER`, `ADMIN`

---

### Card

**Table**: `cards`
**Soft delete**: `deleted_at` (constitution mandate — user-visible entity)

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | UUID | PK, NOT NULL | Auto-generated |
| `name` | VARCHAR(255) | UNIQUE, NOT NULL | Card display name |
| `description` | TEXT | NULLABLE | Card lore/rules text |
| `rarity` | VARCHAR(20) | NOT NULL | Enum: `COMMON`, `RARE`, `EPIC`, `LEGENDARY` |
| `edition` | VARCHAR(100) | NULLABLE | e.g., "First Edition", "Limited" |
| `image_url` | VARCHAR(500) | NULLABLE | External image reference |
| `card_type` | VARCHAR(50) | NOT NULL | Enum: `MONSTER`, `SPELL`, `TRAP` (extensible) |
| `created_at` | TIMESTAMP | NOT NULL | Auto-set on insert |
| `updated_at` | TIMESTAMP | NOT NULL | Auto-set on update |
| `deleted_at` | TIMESTAMP | NULLABLE | Soft-delete timestamp |

**Indexes**:
- `idx_cards_name` on `name` (for search)
- `idx_cards_rarity` on `rarity` (for filter)
- `idx_cards_card_type` on `card_type` (for filter)
- `idx_cards_deleted_at` on `deleted_at`

---

### UserCard (Inventory Entry)

**Table**: `user_cards`
**Soft delete**: None (hard delete — internal ledger rows)

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | UUID | PK, NOT NULL | Auto-generated |
| `user_id` | UUID | FK → users(id), NOT NULL | Card owner |
| `card_id` | UUID | FK → cards(id), NOT NULL | Owned card |
| `quantity` | INTEGER | NOT NULL, CHECK ≥ 1 | Must never go below 1 (row deleted when 0) |
| `acquired_at` | TIMESTAMP | NOT NULL | When first acquired |
| `acquired_from` | VARCHAR(20) | NOT NULL | Enum: `SYSTEM`, `TRADE`, `PURCHASE` |
| `created_at` | TIMESTAMP | NOT NULL | Auto-set on insert |
| `updated_at` | TIMESTAMP | NOT NULL | Auto-set on update |

**Unique constraint**: `(user_id, card_id)` — one row per user-card pair; quantity increments.

**Indexes**:
- `idx_user_cards_user_id` on `user_id`
- `idx_user_cards_card_id` on `card_id`

**Business rules enforced at service layer**:
- Quantity MUST NOT be decremented below 0; row is removed when quantity reaches 0.
- `acquired_at` is set only on the first insertion; subsequent quantity increases do not update it.

---

### Trade

**Table**: `trades`
**Soft delete**: `deleted_at` (constitution mandate — user-visible entity)

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | UUID | PK, NOT NULL | Auto-generated |
| `offerer_id` | UUID | FK → users(id), NOT NULL | User who created the offer |
| `receiver_id` | UUID | FK → users(id), NOT NULL | User who received the offer |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | See state machine below |
| `created_at` | TIMESTAMP | NOT NULL | Auto-set on insert; used for 7-day expiry check |
| `updated_at` | TIMESTAMP | NOT NULL | Auto-set on update |
| `accepted_at` | TIMESTAMP | NULLABLE | Set when status → ACCEPTED |
| `completed_at` | TIMESTAMP | NULLABLE | Set when status → COMPLETED |
| `deleted_at` | TIMESTAMP | NULLABLE | Soft-delete timestamp |
| `idempotency_key` | VARCHAR(36) | UNIQUE, NULLABLE | Idempotency key from client (24h window) |

**Indexes**:
- `idx_trades_offerer_id` on `offerer_id`
- `idx_trades_receiver_id` on `receiver_id`
- `idx_trades_status` on `status`
- `idx_trades_created_at` on `created_at` (for expiry scheduler query)

**State Machine**:

```
                    ┌─────────────┐
                    │   PENDING   │
                    └──────┬──────┘
           ┌───────────────┼────────────────┐
           ▼               ▼                ▼
      ┌──────────┐   ┌──────────┐   ┌────────────┐
      │ REJECTED │   │CANCELLED │   │  ACCEPTED  │
      └──────────┘   └──────────┘   └─────┬──────┘
                                          │
                              ┌───────────┴────────────┐
                              ▼                        ▼
                        ┌──────────┐           ┌──────────┐
                        │COMPLETED │           │  FAILED  │
                        └──────────┘           └──────────┘
```

**Valid transitions**:
- `PENDING` → `ACCEPTED` (receiver accepts)
- `PENDING` → `REJECTED` (receiver rejects)
- `PENDING` → `CANCELLED` (offerer cancels, or 7-day expiry)
- `ACCEPTED` → `COMPLETED` (Kafka consumer: inventory exchange succeeds)
- `ACCEPTED` → `FAILED` (Kafka consumer: inventory exchange fails — compensating action)

---

### TradeItem

**Table**: `trade_items`
**Soft delete**: None (immutable once created; parent Trade carries lifecycle)

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | UUID | PK, NOT NULL | Auto-generated |
| `trade_id` | UUID | FK → trades(id), NOT NULL | Parent trade |
| `card_id` | UUID | FK → cards(id), NOT NULL | Card involved |
| `quantity` | INTEGER | NOT NULL, CHECK ≥ 1 | Cards transacted |
| `side` | VARCHAR(10) | NOT NULL | Enum: `OFFER` (offerer gives), `REQUEST` (offerer wants) |
| `created_at` | TIMESTAMP | NOT NULL | Auto-set on insert |

**Indexes**:
- `idx_trade_items_trade_id` on `trade_id`
- `idx_trade_items_card_id` on `card_id`

---

## Entity Relationships

```
User (1) ──────< UserCard (N)
User (1) ──────< Trade (N)  [as offerer_id]
User (1) ──────< Trade (N)  [as receiver_id]
Trade (1) ─────< TradeItem (N)
Card (1) ──────< UserCard (N)
Card (1) ──────< TradeItem (N)
```

---

## Flyway Migration Files

| File | Purpose |
|------|---------|
| `V1__create_users.sql` | `users` table + indexes |
| `V2__create_cards.sql` | `cards` table + indexes |
| `V3__create_user_cards.sql` | `user_cards` table + unique constraint + indexes |
| `V4__create_trades.sql` | `trades` table + indexes |
| `V5__create_trade_items.sql` | `trade_items` table + indexes |

---

## Redis Key Reference

| Key Pattern | TTL | Purpose |
|-------------|-----|---------|
| `card:catalog:<hash>` | 1 hour | Paginated card list cache (hash of query params) |
| `card:detail:<cardId>` | 1 hour | Individual card detail cache |
| `user:profile:<userId>` | 30 min | User profile cache |
| `ratelimit:api:<userId>` | 60 sec | Sliding window API rate limit counter |
| `ratelimit:login:<email>` | 900 sec | Login attempt rate limit counter |
| `reset:<token>` | 900 sec | Password reset token → userId mapping |
| `idempotency:<key>` | 86400 sec | Trade creation idempotency key → tradeId |

---

## Kafka Topics

| Topic | Producer | Consumer(s) | Group |
|-------|----------|-------------|-------|
| `trading.user.registered` | `EventPublisher` | `NotificationConsumer` | `notification-group` |
| `trading.user.updated` | `EventPublisher` | `NotificationConsumer` | `notification-group` |
| `trading.trade.created` | `EventPublisher` | `NotificationConsumer` | `notification-group` |
| `trading.trade.accepted` | `EventPublisher` | `TradeInventoryConsumer`, `NotificationConsumer` | `trade-inventory-group`, `notification-group` |
| `trading.trade.rejected` | `EventPublisher` | `NotificationConsumer` | `notification-group` |
| `trading.trade.completed` | `EventPublisher` (from consumer) | `NotificationConsumer` | `notification-group` |
| `trading.trade.cancelled` | `EventPublisher` | `NotificationConsumer` | `notification-group` |
| `trading.card.added` | `EventPublisher` (from consumer) | — | — |
| `trading.card.removed` | `EventPublisher` (from consumer) | — | — |

**DLT (Dead Letter Topics)**: Each topic has a corresponding `<topic>.DLT` for messages that fail after 3 retry attempts.
