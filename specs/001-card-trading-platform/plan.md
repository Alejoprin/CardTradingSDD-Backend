# Implementation Plan: Card Trading Platform

**Branch**: `001-card-trading-platform` | **Date**: 2026-04-23 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/001-card-trading-platform/spec.md`

## Summary

A REST API backend for a collectible card trading platform. Users register and authenticate via JWT, browse a Redis-cached card catalog, manage their card inventory, and create bilateral trade offers that are validated, persisted, and resolved asynchronously via Kafka events. The system is built as a modular monolith (Spring Boot 3.2 / Java 17) with PostgreSQL for persistence, Kafka for event-driven trade processing, Redis for caching and rate limiting, and Flyway for schema management.

## Technical Context

**Language/Version**: Java 17 (LTS)
**Primary Dependencies**: Spring Boot 3.2.x (Web, Data JPA, Security, Validation), Spring Kafka 3.x, Spring Data Redis 3.x, JJWT 0.12.x, Lombok, Flyway 10.x, SpringDoc OpenAPI 3 (Swagger UI), Spring Mail
**Storage**: PostgreSQL 15+ (primary relational store), Redis 7+ (cache, rate limiting, password-reset tokens)
**Testing**: JUnit 5, Mockito, Testcontainers (PostgreSQL, Kafka, Redis), MockMvc, REST Assured; 80% minimum coverage gate
**Target Platform**: Linux server via Docker & Docker Compose
**Project Type**: REST API web service (modular monolith)
**Performance Goals**: p95 API response time < 200ms; 1,000 concurrent users
**Constraints**: Max 50 trades/user/day; max 20 items/trade; all list endpoints paginated; no unbounded queries; rate limit 100 req/min/user via Redis
**Scale/Scope**: ~1,000 concurrent users; V1 single-region deployment

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. API-First Design | ✅ PASS | All endpoints under `/api/v1`; contracts defined in `contracts/`; SpringDoc OpenAPI 3 mandated |
| II. Data Integrity & Transactional Consistency | ✅ PASS | All trade operations in DB transactions; Flyway migrations; soft-delete (`deleted_at`) on User, Card, Trade; idempotency keys on trade mutation endpoints |
| III. Test-First Development | ✅ PASS | JUnit 5 + Mockito (unit); Testcontainers (integration); MockMvc / REST Assured (contract); 80% gate |
| IV. Security & Authorization | ✅ PASS | JWT (1h access / 7d refresh); BCrypt; ROLE_USER / ROLE_ADMIN; sole open path `/api/v1/auth/**` |
| V. Observability | ✅ PASS | Structured JSON logs; correlation ID per request; `/health` + `/ready` endpoints; business events as Kafka audit events |
| Event-Driven Architecture | ✅ PASS | Topic naming `trading.{entity}.{action}`; DLQ; 3 retries with exponential backoff; idempotent consumers |
| Caching Standards | ✅ PASS | Card catalog TTL 1h; user profile TTL 30min; Redis-backed; LRU eviction; namespaced keys |
| Code Standards | ✅ PASS | Entities never returned from controllers (DTO pattern); business logic only in services; English identifiers |
| Performance & Operational Constraints | ✅ PASS | < 200ms p95; pagination enforced; rate limiting via Redis; server-side business limit enforcement |

**Constitution Check Result**: ALL GATES PASS — proceed to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/001-card-trading-platform/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── auth.md
│   ├── users.md
│   ├── cards.md
│   ├── trades.md
│   └── admin.md
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```text
src/
└── main/
    ├── java/com/cardtrading/
    │   ├── CardTradingApplication.java
    │   ├── auth/
    │   │   ├── controller/AuthController.java
    │   │   ├── dto/
    │   │   │   ├── RegisterRequest.java
    │   │   │   ├── LoginRequest.java
    │   │   │   ├── TokenResponse.java
    │   │   │   └── RefreshRequest.java
    │   │   ├── entity/User.java
    │   │   ├── repository/UserRepository.java
    │   │   └── service/AuthService.java
    │   ├── card/
    │   │   ├── controller/CardController.java
    │   │   ├── dto/
    │   │   │   ├── CardDto.java
    │   │   │   ├── CardRequest.java
    │   │   │   └── CardResponse.java
    │   │   ├── entity/Card.java
    │   │   ├── repository/CardRepository.java
    │   │   └── service/CardService.java
    │   ├── trade/
    │   │   ├── controller/TradeController.java
    │   │   ├── dto/
    │   │   │   ├── CreateTradeRequest.java
    │   │   │   ├── TradeResponse.java
    │   │   │   └── TradeItemDto.java
    │   │   ├── entity/
    │   │   │   ├── Trade.java
    │   │   │   └── TradeItem.java
    │   │   ├── repository/
    │   │   │   ├── TradeRepository.java
    │   │   │   └── TradeItemRepository.java
    │   │   └── service/TradeService.java
    │   ├── inventory/
    │   │   ├── controller/InventoryController.java
    │   │   ├── dto/
    │   │   │   └── UserCardDto.java
    │   │   ├── entity/UserCard.java
    │   │   ├── repository/UserCardRepository.java
    │   │   └── service/InventoryService.java
    │   ├── event/
    │   │   ├── model/
    │   │   │   ├── UserRegisteredEvent.java
    │   │   │   ├── TradeCreatedEvent.java
    │   │   │   ├── TradeAcceptedEvent.java
    │   │   │   ├── TradeRejectedEvent.java
    │   │   │   ├── TradeCompletedEvent.java
    │   │   │   └── TradeCancelledEvent.java
    │   │   ├── producer/EventPublisher.java
    │   │   └── consumer/
    │   │       ├── TradeInventoryConsumer.java
    │   │       └── NotificationConsumer.java
    │   ├── admin/
    │   │   ├── controller/AdminController.java
    │   │   └── service/AdminService.java
    │   └── shared/
    │       ├── config/
    │       │   ├── SecurityConfig.java
    │       │   ├── KafkaConfig.java
    │       │   ├── RedisConfig.java
    │       │   └── OpenApiConfig.java
    │       ├── exception/
    │       │   ├── GlobalExceptionHandler.java
    │       │   ├── ResourceNotFoundException.java
    │       │   ├── BusinessRuleException.java
    │       │   └── UnauthorizedException.java
    │       ├── security/
    │       │   ├── JwtService.java
    │       │   └── JwtAuthFilter.java
    │       └── util/CorrelationIdFilter.java
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        └── db/migration/
            ├── V1__create_users.sql
            ├── V2__create_cards.sql
            ├── V3__create_user_cards.sql
            ├── V4__create_trades.sql
            └── V5__create_trade_items.sql

src/test/
└── java/com/cardtrading/
    ├── auth/
    │   ├── AuthControllerTest.java    (contract — MockMvc)
    │   └── AuthServiceTest.java       (unit — Mockito)
    ├── card/
    │   ├── CardControllerTest.java
    │   └── CardServiceTest.java
    ├── trade/
    │   ├── TradeControllerTest.java
    │   └── TradeServiceTest.java
    └── integration/
        ├── AuthIntegrationTest.java   (Testcontainers)
        ├── TradeFlowIntegrationTest.java
        └── KafkaConsumerIntegrationTest.java

docker/
└── docker-compose.yml
```

**Structure Decision**: Single-project modular monolith with package-level module separation (`auth`, `card`, `trade`, `inventory`, `event`, `admin`, `shared`). Each module is internally complete (controller → service → repository → entity/dto) and can be extracted to a standalone microservice without restructuring its internal layout, per constitution requirement.

## Complexity Tracking

No constitution violations. All patterns used (Repository, Service Layer, DTO, Event-Driven) are mandated by the constitution and are not introduced speculatively.
