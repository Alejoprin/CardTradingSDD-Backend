# Research: Card Trading Platform

**Phase**: 0 — Research & Decision Log
**Date**: 2026-04-23
**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

---

## Decision 1: JWT Library

**Decision**: JJWT (`io.jsonwebtoken:jjwt-api` 0.12.x) for JWT generation and validation.

**Rationale**: JJWT is the de-facto standard for manual JWT handling in Spring Boot when Spring Authorization Server is not used. It provides full control over token claims, signing algorithms (HS256/RS256), and expiry. It integrates cleanly with a custom `JwtAuthFilter` + `JwtService` pattern, keeping the security layer transparent and testable.

**Alternatives considered**:
- Spring Security OAuth2 Resource Server: Adds significant configuration complexity for a system that does not federate identity externally. Overkill for V1.
- Nimbus JOSE + JWT: More verbose API; JJWT is more idiomatic for Spring Boot projects of this size.

---

## Decision 2: Redis Cache Strategy

**Decision**: Spring Cache abstraction (`@Cacheable`, `@CacheEvict`) backed by `RedisCacheManager`, with explicit TTL configuration per cache region.

**Rationale**: Spring Cache abstractions decouple business logic from caching infrastructure. TTLs are configured centrally in `RedisConfig` (card catalog: 1h; user profile: 30min), matching constitution mandates. `@CacheEvict` on admin mutations ensures consistency when cards are updated or deleted.

**Cache key namespacing**:
- Card catalog list: `card:catalog:<page>:<size>:<filters>`
- Card detail: `card:detail:<cardId>`
- User profile: `user:profile:<userId>`

**Alternatives considered**:
- Manual `RedisTemplate` operations: More control but couples service methods to Redis; harder to test.
- Caffeine (in-memory): Does not survive pod restarts; incompatible with horizontal scaling goal.

---

## Decision 3: Kafka Consumer Group Strategy

**Decision**: Two consumer groups, each with a single consumer class:
1. `trade-inventory-group` → `TradeInventoryConsumer` (listens to `trading.trade.accepted`; processes inventory exchange)
2. `notification-group` → `NotificationConsumer` (listens to all trade and user events; sends email notifications)

**Rationale**: Separate consumer groups allow each concern (inventory mutation vs. notification delivery) to process events independently and at its own pace. A failure in notification delivery does not block inventory processing.

**Idempotency**: Each consumer checks whether the event has already been processed (by trade status or a deduplication key in Redis) before mutating state. Duplicate deliveries are safe.

**Retry & DLQ**: Spring Kafka `DefaultErrorHandler` with `ExponentialBackOffWithMaxRetries(3)` on each consumer. Failed records after 3 attempts are routed to a `<topic>.DLT` (Dead Letter Topic).

**Alternatives considered**:
- Single consumer group for all events: Creates tight coupling; a slow notification handler would delay inventory updates.
- Kafka Streams: Unnecessary complexity for V1's event volume and processing requirements.

---

## Decision 4: Trade Expiry (7-day Auto-Cancel)

**Decision**: Spring `@Scheduled` task running every 15 minutes, querying `trades` where `status = PENDING` and `created_at < NOW() - INTERVAL '7 days'`, bulk-updating status to `CANCELLED`.

**Rationale**: Simple, reliable, and requires no additional infrastructure. The 15-minute polling interval is acceptable given the 7-day expiry window (< 0.2% precision loss). Each expired trade publishes a `trading.trade.cancelled` event so downstream consumers (notifications) are informed.

**Alternatives considered**:
- Kafka timer / delayed message: Not natively supported in Kafka without additional plugins (e.g., Kafka Delay Queue). Adds complexity.
- Database scheduled job (pg_cron): Introduces external dependency and removes expiry logic from application observability.

---

## Decision 5: Password Reset Flow

**Decision**: Token-based reset stored in Redis with a 15-minute TTL.

**Flow**:
1. User submits email → system generates a UUID token, stores `reset:<token> → userId` in Redis (TTL 15min).
2. System sends email with a reset link containing the token.
3. User submits new password + token → system validates token exists in Redis, updates password, deletes token.

**Rationale**: Redis TTL natively enforces token expiry without a scheduled cleanup job. Tokens are single-use (deleted on consumption). No sensitive data (password) is in the token itself.

**Alternatives considered**:
- JWT-encoded reset token: Token cannot be revoked server-side once issued; does not support single-use guarantee without additional state.
- Database token table: Works but requires scheduled cleanup; Redis TTL is simpler.

---

## Decision 6: Email Notification Delivery

**Decision**: Spring Mail (`spring-boot-starter-mail`) with SMTP configuration, triggered by `NotificationConsumer`.

**Rationale**: Spring Mail is the standard, dependency-free email solution for Spring Boot V1 projects. It integrates with any SMTP provider (Gmail SMTP, SendGrid SMTP relay, AWS SES SMTP) via `application.yml` configuration, keeping the implementation provider-agnostic.

**Retry handling**: Email sending is wrapped in the Kafka consumer retry mechanism (3 attempts). Failed deliveries after retries go to the DLT for manual inspection.

**Alternatives considered**:
- SendGrid / Mailgun HTTP API client: Adds a third-party SDK dependency. SMTP relay from these providers achieves the same result via Spring Mail, deferring the provider choice to configuration.
- Async `@Async` Spring task: Not durable — messages lost on pod restart. Kafka consumer is the durable, retryable alternative.

---

## Decision 7: Rate Limiting Strategy

**Decision**: Redis-backed sliding window counter per `(userId, endpoint-category)`, enforced in a `RateLimitFilter` or via Spring Security's method-level interceptor using `RedisTemplate`.

**Rate limits** (per constitution):
- General API: 100 requests/minute per user
- Login: 5 attempts/15 minutes per user (separate counter key `ratelimit:login:<email>`)

**Key pattern**: `ratelimit:api:<userId>` (TTL 60s), `ratelimit:login:<email>` (TTL 900s)

**Alternatives considered**:
- Bucket4j with Redis: Production-grade library for this use case; appropriate if rate limiting grows complex. Deferred to V2 to avoid over-engineering V1.
- In-memory counter: Does not work correctly across horizontal pod replicas.

---

## Decision 8: Soft Delete Scope

**Decision**: Apply `deleted_at` soft-delete to `User`, `Card`, and `Trade` entities. `UserCard` and `TradeItem` use hard deletes (they are internal ledger rows without independent user-visible identity).

**Rationale**: Constitution mandates soft deletes for "user-visible entities." Cards and Users are directly browsed/searched. Trades are visible in history. UserCard and TradeItem rows are derived from trade completion and do not need independent audit trails — their history is captured in the parent Trade.

**Implementation**: JPA `@Where(clause = "deleted_at IS NULL")` annotation on `User`, `Card`, and `Trade` entities filters soft-deleted rows from all queries transparently.

---

## Decision 9: Idempotency Keys

**Decision**: Idempotency enforced on `POST /api/v1/trades` (trade creation) via an `Idempotency-Key` request header (UUID). The key is stored in Redis (`idempotency:<key>`) with a 24-hour TTL alongside the resulting trade ID. Repeated requests with the same key return the original response.

**Rationale**: Constitution mandates idempotency keys on mutating endpoints that can be retried. Trade creation is the primary mutating endpoint with retry risk (network timeout after DB write but before client receives response).

**Alternatives considered**:
- Database unique constraint on offerer+receiver+cards: Too rigid — users legitimately create repeated identical offers on different days.
- No idempotency: Violates constitution mandate.
