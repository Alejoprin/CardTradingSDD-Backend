# Tasks: Card Trading Platform

**Input**: Design documents from `specs/001-card-trading-platform/`
**Prerequisites**: plan.md ✅ | spec.md ✅ | research.md ✅ | data-model.md ✅ | contracts/ ✅

**Tests**: Included — the project constitution mandates test-first development (Red-Green-Refactor) as a non-negotiable gate. Tests are written before implementation in every user story phase.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US7, mapped from spec.md priorities P1–P7)
- Every task includes an exact file path

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project skeleton, build system, and local dev environment — no business logic yet.

- [x] T001 Initialize Spring Boot 3.2 Maven project with `pom.xml` declaring all dependencies (Spring Web, Data JPA, Security, Validation, Kafka, Data Redis, Mail, Thymeleaf, Flyway, JJWT 0.12.x, Lombok, SpringDoc OpenAPI 3, Testcontainers, JUnit 5, Mockito, REST Assured, Jacoco) in `pom.xml`
- [x] T002 Create `docker/docker-compose.yml` with services: PostgreSQL 15, Kafka + Zookeeper, Redis 7 (mapped ports, health checks, named volumes)
- [x] T003 [P] Create `src/main/resources/application.yml` with datasource, JPA, Kafka, Redis, mail, and Flyway config placeholders (all secrets as `${ENV_VAR}` references)
- [x] T004 [P] Create `src/main/resources/application-dev.yml` with local dev overrides pointing to Docker services
- [x] T005 [P] Create main entry point `src/main/java/com/cardtrading/CardTradingApplication.java` with `@SpringBootApplication`
- [x] T006 Configure Jacoco Maven plugin in `pom.xml` with 80% line coverage gate on `mvn verify`

**Checkpoint**: `mvn spring-boot:run -Dspring-boot.run.profiles=dev` starts without errors against Docker services.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Database schema, JPA entities, shared security infrastructure, and Kafka/Redis config that ALL user stories depend on. No user story work begins until this phase is complete.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Database Migrations

- [x] T007 Create Flyway migration `src/main/resources/db/migration/V1__create_users.sql` — `users` table with all columns, unique indexes on `email` and `username`, partial index on `deleted_at`
- [x] T008 [P] Create Flyway migration `src/main/resources/db/migration/V2__create_cards.sql` — `cards` table with indexes on `name`, `rarity`, `card_type`, `deleted_at`
- [x] T009 [P] Create Flyway migration `src/main/resources/db/migration/V3__create_user_cards.sql` — `user_cards` table with `UNIQUE(user_id, card_id)` constraint and indexes on `user_id`, `card_id`
- [x] T010 [P] Create Flyway migration `src/main/resources/db/migration/V4__create_trades.sql` — `trades` table with indexes on `offerer_id`, `receiver_id`, `status`, `created_at`, `UNIQUE(idempotency_key)`
- [x] T011 [P] Create Flyway migration `src/main/resources/db/migration/V5__create_trade_items.sql` — `trade_items` table with indexes on `trade_id`, `card_id`

### JPA Entities

- [x] T012 Create `src/main/java/com/cardtrading/auth/entity/User.java` — JPA entity with all columns, `@Where("deleted_at IS NULL")`, BCrypt-compatible password field, `Role` enum, `@PrePersist`/`@PreUpdate` for timestamps
- [x] T013 [P] Create `src/main/java/com/cardtrading/card/entity/Card.java` — JPA entity with `Rarity` and `CardType` enums, `@Where("deleted_at IS NULL")`, audit timestamps
- [x] T014 [P] Create `src/main/java/com/cardtrading/inventory/entity/UserCard.java` — JPA entity with `@ManyToOne` to User and Card, `AcquisitionSource` enum, quantity field
- [x] T015 [P] Create `src/main/java/com/cardtrading/trade/entity/Trade.java` — JPA entity with `TradeStatus` enum, two FK references to User, nullable `acceptedAt`/`completedAt`, `@Where("deleted_at IS NULL")`
- [x] T016 [P] Create `src/main/java/com/cardtrading/trade/entity/TradeItem.java` — JPA entity with `@ManyToOne` to Trade and Card, `TradeSide` enum

### Shared Security Infrastructure

- [x] T017 Implement `src/main/java/com/cardtrading/shared/security/JwtService.java` — generate access tokens (1h), refresh tokens (7d), validate and parse claims using JJWT 0.12.x
- [x] T018 Implement `src/main/java/com/cardtrading/shared/security/JwtAuthFilter.java` — `OncePerRequestFilter` that extracts Bearer token, validates via `JwtService`, sets `SecurityContextHolder`
- [x] T019 Create `src/main/java/com/cardtrading/shared/config/SecurityConfig.java` — `SecurityFilterChain`: permit `/api/v1/auth/**` and `/swagger-ui/**`, protect all others; add `JwtAuthFilter`; configure BCrypt `PasswordEncoder` bean; disable sessions (stateless); configure CORS
- [x] T020 [P] Implement `src/main/java/com/cardtrading/shared/util/CorrelationIdFilter.java` — generate/propagate `X-Trace-Id` header per request, store in MDC for structured logging

### API Rate Limiting (Constitution MUST)

- [x] T020a [P] Implement `src/main/java/com/cardtrading/shared/security/ApiRateLimitFilter.java` — `OncePerRequestFilter` enforcing 100 requests/minute per authenticated user via Redis sliding window counter; key pattern `ratelimit:api:<userId>` with 60s TTL; throw `TooManyRequestsException` (429) on breach; skip for unauthenticated paths (`/api/v1/auth/**`); register in `SecurityConfig` filter chain after `JwtAuthFilter`

### Shared Infrastructure Config

- [x] T021 [P] Create `src/main/java/com/cardtrading/shared/config/KafkaConfig.java` — producer factory (JSON serializer), consumer factories for `trade-inventory-group` and `notification-group`, `DefaultErrorHandler` with `ExponentialBackOffWithMaxRetries(3)` routing to DLT topics
- [x] T022 [P] Create `src/main/java/com/cardtrading/shared/config/RedisConfig.java` — `RedisCacheManager` with per-region TTLs (`card:catalog` 1h, `card:detail` 1h, `user:profile` 30min); LRU eviction policy; namespaced key prefix
- [x] T023 [P] Create `src/main/java/com/cardtrading/shared/config/OpenApiConfig.java` — SpringDoc `OpenAPI` bean with JWT Bearer security scheme, API info (`/api/v1` base, V1)
- [x] T024 [P] Implement `src/main/java/com/cardtrading/shared/exception/GlobalExceptionHandler.java` — `@RestControllerAdvice` handling `ResourceNotFoundException` (404), `BusinessRuleException` (422), `UnauthorizedException` (401), `TooManyRequestsException` (429), `MethodArgumentNotValidException` (400), generic 500; all responses include `traceId` from MDC
- [x] T025 [P] Create custom exception classes: `ResourceNotFoundException`, `BusinessRuleException`, `UnauthorizedException`, `TooManyRequestsException` in `src/main/java/com/cardtrading/shared/exception/`
- [x] T026 [P] Implement `src/main/java/com/cardtrading/event/producer/EventPublisher.java` — generic `KafkaTemplate`-based publisher with structured event serialization; logs topic + traceId on publish
- [x] T027 [P] Create all Kafka event model classes in `src/main/java/com/cardtrading/event/model/`: `UserRegisteredEvent`, `TradeCreatedEvent`, `TradeAcceptedEvent`, `TradeRejectedEvent`, `TradeCompletedEvent`, `TradeCancelledEvent`

**Checkpoint**: Flyway migrations run cleanly, entities load without errors, Spring context starts with security, Kafka, and Redis beans. All `mvn test` passes with zero business logic yet.

---

## Phase 3: User Story 1 — User Registration and Login (Priority: P1) 🎯 MVP

**Goal**: A user can register, log in, refresh their token, log out, and reset their password. All downstream stories depend on having an authenticated user.

**Independent Test**: Register a new user → log in → access a protected endpoint with the access token → verify 401 with expired/missing token → rate-limit login after 5 failures → reset password via token.

### Tests for User Story 1 ⚠️ Write and confirm FAIL before implementation

- [x] T028 [P] [US1] Contract test `src/test/java/com/cardtrading/auth/AuthControllerTest.java` — MockMvc tests for all auth endpoints: `POST /register` (201, 400, 409), `POST /login` (200, 401, 429), `POST /refresh` (200, 401), `POST /logout` (200), `POST /password/reset` (200), `POST /password/reset/confirm` (200, 400)
- [x] T029 [P] [US1] Unit test `src/test/java/com/cardtrading/auth/AuthServiceTest.java` — Mockito tests for `register()` (happy path, duplicate email, duplicate username, weak password), `login()` (valid, bad password, banned user), `refreshToken()` (valid, expired), `initiatePasswordReset()`, `confirmPasswordReset()` (valid token, expired token)

### Implementation for User Story 1

- [x] T030 [US1] Create `src/main/java/com/cardtrading/auth/repository/UserRepository.java` — `JpaRepository<User, UUID>` with finders: `findByEmail`, `findByUsername`, `existsByEmail`, `existsByUsername`
- [x] T031 [P] [US1] Create auth DTOs in `src/main/java/com/cardtrading/auth/dto/`: `RegisterRequest` (Bean Validation annotations), `LoginRequest`, `TokenResponse`, `RefreshRequest`, `PasswordResetRequest`, `PasswordResetConfirmRequest`, `UserResponse`
- [x] T032 [US1] Implement `src/main/java/com/cardtrading/auth/service/AuthService.java` — `register()` (validate uniqueness, BCrypt hash, save, publish `UserRegisteredEvent`), `login()` (verify credentials, check ban, generate tokens), `refreshToken()`, `logout()` (blacklist refresh token in Redis), `initiatePasswordReset()` (store UUID token in Redis 15min TTL, send email), `confirmPasswordReset()` (validate token, update password, delete token)
- [x] T033 [US1] Implement rate limiting in `src/main/java/com/cardtrading/shared/security/LoginRateLimitService.java` — Redis sliding window counter with key `ratelimit:login:<email>` (5 attempts / 900s TTL); throw `TooManyRequestsException` on breach
- [x] T034 [US1] Implement `src/main/java/com/cardtrading/auth/controller/AuthController.java` — all six auth endpoints per `contracts/auth.md`; inject `AuthService`, delegate all logic; return correct HTTP status codes
- [x] T035 [US1] Integration test `src/test/java/com/cardtrading/integration/AuthIntegrationTest.java` — Testcontainers (PostgreSQL + Redis) full register → login → protected endpoint → rate limit flow end-to-end

**Checkpoint**: `POST /api/v1/auth/register` and `POST /api/v1/auth/login` work; JWT is returned and accepted on protected endpoints; rate limiting blocks the 6th login attempt. All T028–T035 tests green.

---

## Phase 4: User Story 2 — Browse and Search the Card Catalog (Priority: P2)

**Goal**: A logged-in user can page through all cards, search by name, and filter by rarity/type/edition; results are served from Redis cache after the first load.

**Independent Test**: Seed 10 cards → list all (paginated) → search by name substring → filter by LEGENDARY rarity → verify cache hit on repeated call → admin soft-deletes a card → card disappears from results.

### Tests for User Story 2 ⚠️ Write and confirm FAIL before implementation

- [x] T036 [P] [US2] Contract test `src/test/java/com/cardtrading/card/CardControllerTest.java` — MockMvc tests for `GET /cards` (pagination, search, filters, 200), `GET /cards/{id}` (200, 404)
- [x] T037 [P] [US2] Unit test `src/test/java/com/cardtrading/card/CardServiceTest.java` — Mockito tests for `listCards()` (all filter combos, cache hit/miss), `getCardById()` (found, not found)

### Implementation for User Story 2

- [x] T038 [US2] Create `src/main/java/com/cardtrading/card/repository/CardRepository.java` — `JpaRepository<Card, UUID>` with `@Query` for name search (ILIKE) and filter combination using `Specification<Card>`
- [x] T039 [P] [US2] Create card DTOs in `src/main/java/com/cardtrading/card/dto/`: `CardSummaryResponse` (list view), `CardDetailResponse` (full detail), `CardRequest` (create/update with Bean Validation)
- [x] T040 [US2] Implement `src/main/java/com/cardtrading/card/service/CardService.java` — `listCards(Pageable, filters)` with `@Cacheable("card:catalog")`, `getCardById(UUID)` with `@Cacheable("card:detail")`; cache key includes all filter parameters
- [x] T041 [US2] Implement `src/main/java/com/cardtrading/card/controller/CardController.java` — `GET /api/v1/cards` and `GET /api/v1/cards/{cardId}` per `contracts/cards.md`; delegate to `CardService`

**Checkpoint**: `GET /api/v1/cards?search=dragon&rarity=LEGENDARY` returns filtered results; second identical call is served from Redis cache. All T036–T041 tests green.

---

## Phase 5: User Story 3 — Manage Personal Inventory (Priority: P3)

**Goal**: A logged-in user can view their own card inventory (with quantity and acquisition metadata) and view another user's public inventory.

**Independent Test**: Grant user 3 cards via direct DB insert → `GET /api/v1/users/{userId}/inventory` → assert correct cards, quantities, and `acquiredFrom` values; repeat for another user's inventory.

### Tests for User Story 3 ⚠️ Write and confirm FAIL before implementation

- [x] T042 [P] [US3] Contract test `src/test/java/com/cardtrading/inventory/InventoryControllerTest.java` — MockMvc tests for `GET /users/{userId}/inventory` (200 with paged content, empty inventory, 404 unknown user)
- [x] T043 [P] [US3] Unit test `src/test/java/com/cardtrading/inventory/InventoryServiceTest.java` — Mockito tests for `getUserInventory()` (multiple cards, empty, unknown user), `addCard()`, `removeCard()` (reduce quantity, delete row at zero)

### Implementation for User Story 3

- [x] T044 [US3] Create `src/main/java/com/cardtrading/inventory/repository/UserCardRepository.java` — `JpaRepository<UserCard, UUID>` with `findByUserIdAndCardId(UUID, UUID)`, `findByUserId(UUID, Pageable)`
- [x] T045 [P] [US3] Create `src/main/java/com/cardtrading/inventory/dto/UserCardDto.java` — fields: `cardId`, `cardName`, `rarity`, `quantity`, `acquiredAt`, `acquiredFrom`
- [x] T046 [US3] Implement `src/main/java/com/cardtrading/inventory/service/InventoryService.java` — `getUserInventory(UUID userId, Pageable)`, `addCard(UUID userId, UUID cardId, int qty, AcquisitionSource)` (upsert quantity), `removeCard(UUID userId, UUID cardId, int qty)` (decrement; delete row if reaches 0; throw if insufficient)
- [x] T047 [US3] Implement `src/main/java/com/cardtrading/inventory/controller/InventoryController.java` — `GET /api/v1/users/{userId}/inventory` per `contracts/users.md`; delegate to `InventoryService`
- [x] T048 [P] [US3] Implement `src/main/java/com/cardtrading/auth/service/UserService.java` — `getUserProfile(UUID requesterId, UUID targetId)` (hide email for non-owner non-admin), `updateUserProfile(UUID userId, UpdateUserRequest)` (check ownership, validate uniqueness)
- [x] T048a [P] [US3] Create `src/main/java/com/cardtrading/auth/dto/UpdateUserRequest.java` — fields: `username` (optional, 3–20 chars), `email` (optional, valid format); Bean Validation annotations
- [x] T048b [US3] Implement `src/main/java/com/cardtrading/auth/controller/UserController.java` — `GET /api/v1/users/{userId}` and `PUT /api/v1/users/{userId}` per `contracts/users.md`; delegate to `UserService`
- [x] T048c [P] [US3] Contract test `src/test/java/com/cardtrading/auth/UserControllerTest.java` — MockMvc tests for `GET /users/{id}` (200, 404, email hidden for non-owner), `PUT /users/{id}` (200, 400, 403 non-owner, 409 duplicate)

**Checkpoint**: `GET /api/v1/users/{userId}/inventory` returns paginated card list with correct metadata. `GET /api/v1/users/{userId}` returns user profile. All T042–T048c tests green.

---

## Phase 6: User Story 4 — Create a Trade Offer (Priority: P4)

**Goal**: User A can create a trade offer specifying cards to give and cards to request from User B. The system validates inventory ownership and enforces all business limits before persisting the offer.

**Independent Test**: Two users each owning cards → User A creates trade → assert PENDING status, both users see it in `GET /trades`; attempt self-trade → 422; exceed daily limit → 422; offer card not owned → 400; idempotent retry with same `Idempotency-Key` → 200 original.

### Tests for User Story 4 ⚠️ Write and confirm FAIL before implementation

- [x] T049 [P] [US4] Contract test `src/test/java/com/cardtrading/trade/TradeControllerTest.java` (create section) — MockMvc tests for `POST /trades` (201 happy path, 400 insufficient cards, 422 self-trade, 422 daily limit, 409 duplicate idempotency key), `GET /trades` (200 paged), `GET /trades/{id}` (200, 403, 404)
- [x] T050 [P] [US4] Unit test `src/test/java/com/cardtrading/trade/TradeServiceTest.java` (createTrade section) — Mockito tests for all validation branches: offerer owns cards, receiver owns requested cards, not self, item count ≤ 20, daily limit ≤ 50, idempotency key dedup

### Implementation for User Story 4

- [x] T051 [US4] Create `src/main/java/com/cardtrading/trade/repository/TradeRepository.java` — finders: `findByOffererId`, `findByReceiverId`, `countByOffererIdAndCreatedAtAfter` (daily limit check), `findByIdAndOffererId`, `findByIdAndReceiverId`
- [x] T052 [P] [US4] Create `src/main/java/com/cardtrading/trade/repository/TradeItemRepository.java` — `findByTradeId`, `findByTradeIdAndSide`
- [x] T053 [P] [US4] Create trade DTOs in `src/main/java/com/cardtrading/trade/dto/`: `CreateTradeRequest` (with nested `TradeItemRequest` list, Bean Validation), `TradeResponse`, `TradeItemDto`, `TradeListResponse`
- [x] T054 [US4] Implement `src/main/java/com/cardtrading/trade/service/TradeService.java#createTrade()` — validate offerer ≠ receiver; validate item count ≤ 20; check daily limit (Redis or DB count); validate offerer owns offered cards; validate receiver owns requested cards; save `Trade` + `TradeItems` in transaction; store idempotency key in Redis (24h); publish `TradeCreatedEvent`
- [x] T055 [US4] Implement `src/main/java/com/cardtrading/trade/controller/TradeController.java` — `POST /api/v1/trades`, `GET /api/v1/trades`, `GET /api/v1/trades/{tradeId}` per `contracts/trades.md`; extract `Idempotency-Key` header; enforce caller visibility rules on GET detail

**Checkpoint**: Two users with seeded inventories can create a trade offer; `GET /api/v1/trades` shows it for both parties; all validation rejections return correct status codes. All T049–T055 tests green.

---

## Phase 7: User Story 5 — Accept, Reject, or Cancel a Trade (Priority: P5)

**Goal**: The receiver can accept or reject a pending trade; the offerer can cancel it. On acceptance, the inventory exchange is processed via Kafka consumer and the trade is marked COMPLETED (or FAILED on error). Trades auto-expire after 7 days.

**Independent Test**: Create PENDING trade → receiver accepts → assert ACCEPTED → Kafka consumer fires → assert both inventories swapped → status COMPLETED; create another → receiver rejects → status REJECTED; create another → offerer cancels → status CANCELLED; simulate 7-day-old PENDING trade → scheduler fires → status CANCELLED.

### Tests for User Story 5 ⚠️ Write and confirm FAIL before implementation

- [x] T056 [P] [US5] Contract test `src/test/java/com/cardtrading/trade/TradeControllerTest.java` (lifecycle section) — MockMvc tests for `PUT /trades/{id}/accept` (200, 403 not-receiver, 409 not-PENDING), `PUT /trades/{id}/reject` (200, 403, 409), `DELETE /trades/{id}` (200, 403 not-offerer, 409)
- [x] T057 [P] [US5] Unit test `src/test/java/com/cardtrading/trade/TradeServiceTest.java` (lifecycle section) — Mockito tests for `acceptTrade()`, `rejectTrade()`, `cancelTrade()` authorization checks, status transition guards
- [x] T058 [P] [US5] Integration test `src/test/java/com/cardtrading/integration/TradeFlowIntegrationTest.java` — Testcontainers (PostgreSQL + Kafka + Redis): full create → accept → consumer processes → assert COMPLETED + inventory swapped; failure scenario → assert FAILED + inventories unchanged

### Implementation for User Story 5

- [x] T059 [US5] Implement `src/main/java/com/cardtrading/trade/service/TradeService.java#acceptTrade()` — assert caller is receiver; assert status is PENDING; update status → ACCEPTED; set `acceptedAt`; publish `TradeAcceptedEvent` in @Transactional
- [x] T060 [US5] Implement `src/main/java/com/cardtrading/trade/service/TradeService.java#rejectTrade()` — assert caller is receiver; assert PENDING; update status → REJECTED; publish `TradeRejectedEvent`
- [x] T061 [US5] Implement `src/main/java/com/cardtrading/trade/service/TradeService.java#cancelTrade()` — assert caller is offerer; assert PENDING; update status → CANCELLED; publish `TradeCancelledEvent`
- [x] T062 [US5] Implement `src/main/java/com/cardtrading/event/consumer/TradeInventoryConsumer.java` — listens to `trading.trade.accepted`; in @Transactional: call `InventoryService.removeCard()` for offered cards from offerer, `addCard()` for offered cards to receiver, `removeCard()` for requested cards from receiver, `addCard()` for requested cards to offerer; on success update trade status → COMPLETED + publish `TradeCompletedEvent`; on failure update status → FAILED (compensating transaction)
- [x] T063 [US5] Add accept/reject/cancel endpoints to `src/main/java/com/cardtrading/trade/controller/TradeController.java` — `PUT /api/v1/trades/{tradeId}/accept`, `PUT /api/v1/trades/{tradeId}/reject`, `DELETE /api/v1/trades/{tradeId}` per `contracts/trades.md`
- [x] T064 [US5] Implement `src/main/java/com/cardtrading/trade/service/TradeExpiryScheduler.java` — `@Scheduled(fixedDelay = 900_000)` queries PENDING trades where `created_at < NOW() - 7 days`; bulk-updates to CANCELLED; publishes `TradeCancelledEvent` per expired trade

**Checkpoint**: Full trade lifecycle works: create → accept → Kafka consumer → COMPLETED + inventories swapped. Reject and cancel paths confirmed. Auto-expiry fires in tests. All T056–T064 tests green.

---

## Phase 8: User Story 6 — Receive Trade Notifications (Priority: P6)

**Goal**: Users receive email notifications on every trade event (offer received, accepted, rejected, completed). Failed deliveries retry 3 times before routing to DLT.

**Independent Test**: Trigger each trade event via Kafka → assert correct email sent to the right user(s) → simulate SMTP failure → assert 3 retries occurred → assert message in DLT topic.

### Tests for User Story 6 ⚠️ Write and confirm FAIL before implementation

- [x] T065 [P] [US6] Integration test `src/test/java/com/cardtrading/integration/KafkaConsumerIntegrationTest.java` — Testcontainers (Kafka): publish each event type → assert `NotificationConsumer` triggered → verify email sent (mock SMTP via GreenMail or Mailhog); publish failing event → verify DLT routing after 3 retries

### Implementation for User Story 6

- [x] T066 [P] [US6] Add Spring Mail dependency and configure `src/main/java/com/cardtrading/shared/config/MailConfig.java` — `JavaMailSender` bean from `application.yml` SMTP properties
- [x] T067 [P] [US6] Create email templates in `src/main/resources/templates/`: `registration-confirmation.html`, `trade-created.html`, `trade-accepted.html`, `trade-rejected.html`, `trade-completed.html` (Thymeleaf; registration template includes username + welcome message; trade templates include trade ID, card names, counterparty username)
- [x] T068 [US6] Implement `src/main/java/com/cardtrading/event/consumer/NotificationConsumer.java` — separate `@KafkaListener` methods for `trading.trade.created`, `trading.trade.accepted`, `trading.trade.rejected`, `trading.trade.completed`, `trading.user.registered`; delegate to `NotificationService`; consumer group: `notification-group`
- [x] T069 [US6] Implement `src/main/java/com/cardtrading/event/service/NotificationService.java` — compose emails per event type using Thymeleaf templates + `JavaMailSender`; look up recipient email from `UserRepository`; log delivery success/failure with traceId

**Checkpoint**: On trade offer creation, receiver gets email; on acceptance, both parties notified; on failure, 3 retries logged, DLT receives message. All T065–T069 tests green.

---

## Phase 9: User Story 7 — Admin Card and User Management (Priority: P7)

**Goal**: Admin users can add/update/delete cards in the catalog, ban/unban users, view all users and trades platform-wide, and retrieve system statistics.

**Independent Test**: Admin JWT → `POST /api/v1/cards` → card appears in catalog; `PUT /api/v1/admin/users/{id}/ban` → that user's next login returns 401; `GET /api/v1/admin/stats` → correct counts; non-admin → 403 on all admin endpoints.

### Tests for User Story 7 ⚠️ Write and confirm FAIL before implementation

- [x] T070 [P] [US7] Contract test `src/test/java/com/cardtrading/admin/AdminControllerTest.java` — MockMvc tests for `GET /admin/users` (200, 403 non-admin), `GET /admin/trades` (200), `PUT /admin/users/{id}/ban` (200, 404, 409, 403), `GET /admin/stats` (200, 403)
- [x] T071 [P] [US7] Contract test extension in `src/test/java/com/cardtrading/card/CardControllerTest.java` — admin-only: `POST /cards` (201, 400, 403, 409), `PUT /cards/{id}` (200, 403, 404), `DELETE /cards/{id}` (204, 403, 404)
- [x] T072 [P] [US7] Unit test `src/test/java/com/cardtrading/admin/AdminServiceTest.java` — Mockito tests for `listUsers()`, `listTrades()`, `banUser()` (ban, unban, already-banned conflict), `getStats()`

### Implementation for User Story 7

- [x] T073 [US7] Implement `src/main/java/com/cardtrading/admin/service/AdminService.java` — `listUsers(Pageable, filters)`, `listAllTrades(Pageable, filters)`, `banUser(UUID, boolean, String)` (set `is_banned`, check idempotency), `getStats()` (aggregate DB counts per entity/status)
- [x] T074 [US7] Implement `src/main/java/com/cardtrading/admin/controller/AdminController.java` — all four admin endpoints per `contracts/admin.md`; secure with `@PreAuthorize("hasRole('ADMIN')")`
- [x] T075 [US7] Extend `src/main/java/com/cardtrading/card/service/CardService.java` — add `createCard(CardRequest)`, `updateCard(UUID, CardRequest)`, `deleteCard(UUID)` with `@CacheEvict` on create/update/delete; publish card events if needed
- [x] T076 [US7] Add admin card CRUD endpoints to `src/main/java/com/cardtrading/card/controller/CardController.java` — `POST /api/v1/cards`, `PUT /api/v1/cards/{cardId}`, `DELETE /api/v1/cards/{cardId}` per `contracts/cards.md`; secure with `@PreAuthorize("hasRole('ADMIN')")`

**Checkpoint**: Admin can manage card catalog (changes visible within cache TTL); ban prevents login; stats endpoint returns correct aggregates. All T070–T076 tests green.

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Observability, documentation, coverage gate, and final validation across all stories.

- [x] T077 [P] Configure structured JSON logging via `src/main/resources/logback-spring.xml` — JSON format in prod profile; include `traceId` (from MDC), `level`, `logger`, `message`, `timestamp`
- [x] T078 [P] Enable Spring Boot Actuator health and readiness endpoints in `src/main/resources/application.yml` — expose `/health` and `/ready`; add Kafka and Redis health indicators
- [x] T079 [P] Verify `src/main/java/com/cardtrading/shared/config/OpenApiConfig.java` — Swagger UI at `/swagger-ui.html` shows all endpoints with JWT Bearer auth; test all contracts are reflected
- [x] T080 Run full test suite `mvn verify` and confirm Jacoco coverage ≥ 80%; add `src/test/resources/application-test.yml` with Testcontainers properties if not already present
- [x] T081 [P] Security review: verify `GlobalExceptionHandler` never returns stack traces, passwords, or tokens in error responses; confirm banned users receive 401 not 403 on login
- [x] T082 [P] Validate `specs/001-card-trading-platform/quickstart.md` by following all steps from scratch against a clean Docker environment — update any inaccurate commands

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — **BLOCKS all user stories**
- **US1 (Phase 3)**: Depends on Phase 2 completion — no other story dependencies
- **US2 (Phase 4)**: Depends on Phase 2 completion — no dependency on US1 (catalog is independent)
- **US3 (Phase 5)**: Depends on Phase 2 + US1 (authentication required to access inventory)
- **US4 (Phase 6)**: Depends on US1 (auth) + US3 (inventory validation)
- **US5 (Phase 7)**: Depends on US4 (trade must exist before accept/reject/cancel)
- **US6 (Phase 8)**: Depends on US5 (trade events must fire to trigger notifications)
- **US7 (Phase 9)**: Depends on US1 (auth) + US2 (card catalog exists to manage)
- **Polish (Phase 10)**: Depends on all desired stories being complete

### User Story Dependencies

| Story | Depends On | Can Parallelize With |
|-------|-----------|---------------------|
| US1 (Auth) | Phase 2 (Foundational) | US2 |
| US2 (Catalog) | Phase 2 (Foundational) | US1 |
| US3 (Inventory) | US1 (auth gate) | US2, US7 |
| US4 (Create Trade) | US1 + US3 | US7 |
| US5 (Trade Lifecycle) | US4 | — |
| US6 (Notifications) | US5 | — |
| US7 (Admin) | US1 + US2 | US3, US4 |

### Within Each User Story

1. Tests MUST be written and confirmed to FAIL before implementation starts
2. Repository → DTO → Service → Controller
3. Unit tests pass → contract tests pass → integration test passes
4. Story complete before moving to next priority

---

## Parallel Opportunities

### Phase 2 (Foundational) — run together

```
Migrations:    T008, T009, T010, T011 (all parallel after T007)
Entities:      T013, T014, T015, T016 (all parallel after T012)
Configs:       T021, T022, T023, T024, T025, T026, T027 (all parallel after T020)
```

### Phase 3 — US1

```
Tests (write first): T028, T029 (parallel)
Implementation:      T030 → T031 (parallel after T030) → T032 → T033 → T034 → T035
```

### Phase 6 — US4

```
Tests (write first): T049, T050 (parallel)
Implementation:      T051 → T052, T053 (parallel after T051) → T054 → T055
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational — **CRITICAL, blocks everything**
3. Write + confirm failing tests T028–T029
4. Complete Phase 3: US1 (registration, login, JWT)
5. **STOP and VALIDATE**: Register a user, log in, confirm JWT works on protected endpoints
6. Demo or deploy

### Incremental Delivery

| Step | Delivers |
|------|---------|
| Phase 1 + 2 | Runnable project, migrated DB, security scaffold |
| + Phase 3 (US1) | Working auth — MVP |
| + Phase 4 (US2) | Browseable card catalog — demo-able feature |
| + Phase 5 (US3) | User inventories — trade precondition met |
| + Phase 6 (US4) | Trade creation — core value proposition |
| + Phase 7 (US5) | Full trade lifecycle — platform is usable |
| + Phase 8 (US6) | Email notifications — async loop closed |
| + Phase 9 (US7) | Admin controls — platform is operable |
| + Phase 10 | Production-ready observability and coverage |

### Parallel Team Strategy (3 developers post-Foundational)

```
Dev A: US1 (auth) → US3 (inventory) → US4 (trade creation)
Dev B: US2 (catalog) → US7 (admin)
Dev C: US5 (trade lifecycle) → US6 (notifications)
```

---

## Notes

- All `[P]` tasks operate on different files with no shared incomplete dependencies — safe to run in parallel
- `[Story]` label maps each task to a user story for traceability and independent delivery
- Constitution gate: every test MUST be written and confirmed failing before implementation of its target
- Commit after each task or logical group using Conventional Commits (`feat:`, `test:`, `fix:`)
- Stop at each **Checkpoint** to validate the story independently before proceeding
- Soft-delete is applied automatically via `@Where` on User, Card, Trade — no extra filter logic needed in queries
- Idempotency key for trade creation: check Redis before DB write; return cached response on duplicate
