<!--
SYNC IMPACT REPORT
==================
Version change: 1.0.0 → 1.1.0 (technology stack ratification + four new sections)
Modified principles:
  - I.  API-First Design: added /api/v1 base URL, canonical status codes, SpringDoc mandate
  - III. Test-First Development: added JUnit 5 + Mockito + Testcontainers, 80% coverage gate
  - IV. Security & Authorization: added JWT (1h/7d), BCrypt, role definitions, auth exclusion path
Added sections:
  - Event-Driven Architecture Standards (new)
  - Caching Standards (new)
  - Code Standards (new)
  - Performance & Operational Constraints (new)
Removed sections: none
Templates requiring updates:
  - .specify/templates/plan-template.md ✅ reviewed — Technical Context placeholders remain
    intentionally generic; concrete defaults now derivable from constitution
  - .specify/templates/spec-template.md ✅ reviewed — no changes required
  - .specify/templates/tasks-template.md ✅ reviewed — path conventions remain generic;
    Java/Spring Boot layout will be specified per feature in plan.md
  - .specify/templates/constitution-template.md ✅ source only, no update needed
Follow-up TODOs: none — all placeholders resolved
-->

# CardTrading Backend Constitution

## Core Principles

### I. API-First Design

Every feature MUST be designed as an HTTP/REST API contract before implementation begins.
API contracts (request/response schemas, status codes, error formats) MUST be defined in
`specs/<feature>/contracts/` and reviewed before any code is written. The base URL for all
endpoints is `/api/v1`. Breaking changes to published contracts MUST follow a versioning
strategy (`/v1/`, `/v2/`, …) and MUST NOT be deployed without a documented migration plan.

Canonical HTTP status codes in use: `200 OK`, `201 Created`, `400 Bad Request`,
`401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict`, `500 Internal Server Error`.
All endpoints MUST be documented via SpringDoc OpenAPI 3 (Swagger UI). All request and
response payloads MUST be JSON.

**Rationale**: Decouples frontend, mobile, and third-party consumers from backend internals;
ensures the system surface area is explicit and reviewable.

### II. Data Integrity & Transactional Consistency

All operations that modify financial or inventory state (trades, listings, purchases,
transfers) MUST execute within database transactions. Partial writes MUST never leave the
system in an inconsistent state. Idempotency keys MUST be enforced for any mutating endpoint
that can be retried by a client. Soft-delete patterns MUST be used for user-visible entities
(cards, listings, offers) via a `deleted_at` timestamp to preserve audit trails. Every entity
MUST carry `created_at` and `updated_at` audit timestamps.

**Rationale**: Card trading involves real asset transfers; data loss or corruption has direct
financial impact on users and erodes trust.

### III. Test-First Development (NON-NEGOTIABLE)

Tests MUST be written and confirmed to fail before implementation begins (Red-Green-Refactor).
The minimum required test coverage per feature:
- Contract tests for every new or modified API endpoint
- Integration tests for every business-critical user journey (using Testcontainers for
  PostgreSQL, Kafka, and Redis)
- Unit tests for domain logic containing conditional branching (JUnit 5 + Mockito)

Test coverage MUST NOT fall below **80%** — this gate is enforced by the CI pipeline.
Tests MUST be committed to the repository before implementation code is merged. No feature
may be marked complete unless all tests are green and the CI pipeline passes.

**Rationale**: Catches regressions early in a domain where correctness (pricing, ownership,
trade validation) is non-negotiable.

### IV. Security & Authorization

Every endpoint MUST enforce authentication via JWT tokens. All endpoints are protected
by default; the sole exclusion is `/api/v1/auth/**`. JWT access tokens expire in **1 hour**;
refresh tokens expire in **7 days**. Authorization MUST be validated server-side on every
request — never inferred from client-supplied data alone. Roles in use: `ROLE_USER`,
`ROLE_ADMIN`. All passwords MUST be hashed with BCrypt before storage. Sensitive data
(passwords, tokens, PII) MUST NOT be logged or returned in error messages. All user inputs
MUST be validated and sanitized before use in queries or business logic. Dependencies MUST
be reviewed for known CVEs before inclusion.

**Rationale**: Marketplace platforms are high-value targets; a single authorization bypass
can expose all user assets.

### V. Observability

Every service MUST emit structured logs (JSON) with a correlation ID on every request.
Errors MUST include severity level, stack context (in non-production), and the correlation
ID. Health-check endpoints (`/health`, `/ready`) MUST be implemented and monitored. Key
business events (trade created, offer accepted, listing published) MUST be emitted as
structured Kafka audit events distinct from operational logs, following the topic naming
convention defined in Event-Driven Architecture Standards.

**Rationale**: Without structured observability, debugging production issues in a real-time
trading system is impractical and incident resolution times increase significantly.

## Technology & Architecture Standards

- **Language**: Java 17 (LTS)
- **Framework**: Spring Boot 3.2.x
- **Build tool**: Maven — `pom.xml` MUST be committed; no SNAPSHOT versions in production
- **Database**: PostgreSQL 15+ via JPA/Hibernate; all schema changes MUST use Flyway migrations
- **Messaging**: Apache Kafka 3.x (Spring Kafka)
- **Caching / Sessions**: Redis 7+
- **Container runtime**: Docker & Docker Compose
- **API documentation**: SpringDoc OpenAPI 3 (Swagger UI at `/swagger-ui.html`)

### Architecture Style

- **Pattern**: Modular Monolith with Event-Driven Architecture
- **Modules**: `auth`, `card`, `trade`, `event`, `shared`
- **Synchronous communication**: REST API under `/api/v1`
- **Asynchronous communication**: Kafka events (see Event-Driven Architecture Standards)
- Each module MUST be designed so it can be extracted into a standalone microservice without
  structural changes to its internal package layout.

### Database Conventions

- **ORM**: JPA/Hibernate
- **Migrations**: Flyway — every schema change MUST have a versioned migration script
- **Naming**: snake_case for tables and columns (e.g., `users`, `created_at`)
- **Soft deletes**: entities MUST use a `deleted_at` timestamp instead of hard deletes
- **Audit columns**: every entity MUST have `created_at` and `updated_at` timestamps

### Environment & Secrets

- All secrets MUST be injected via environment variables; no secrets in source code or
  committed config files.
- **Branching**: Sequential feature branches (`001-feature-name`, `002-feature-name`, …)
  as configured in `.specify/init-options.json`.

## Event-Driven Architecture Standards

- **Topic naming pattern**: `{domain}.{entity}.{action}`
  (e.g., `trading.trade.created`, `trading.user.registered`)
- **Canonical events**: `trading.user.registered`, `trading.trade.created`,
  `trading.trade.completed` — additional events MUST follow the same pattern
- **Consumer requirements**:
  - Consumers MUST be idempotent (duplicate delivery MUST be safe)
  - MUST implement a Dead Letter Queue (DLQ) for unrecoverable failures
  - MUST retry 3 times with exponential backoff before routing to DLQ

## Caching Standards (Redis)

- Card catalog cache TTL: **1 hour**
- User profile cache TTL: **30 minutes**
- Session storage: Redis-backed
- Eviction policy: LRU
- Cache keys MUST be namespaced by entity type (e.g., `card:catalog:*`, `user:profile:{id}`)

## Code Standards

- **Language of code**: English — all identifiers, comments, and commit messages MUST be in English
- **Naming conventions**:
  - Classes: PascalCase
  - Methods and fields: camelCase
  - Constants: UPPER_SNAKE_CASE
- **DTO naming**:
  - Data Transfer Objects: suffix `Dto` (e.g., `UserDto`)
  - Request objects: suffix `Request` (e.g., `CreateTradeRequest`)
  - Response objects: suffix `Response` (e.g., `TradeResponse`)
- **Layering rules**:
  - Entities MUST NOT be returned directly from controllers — always use DTOs
  - Business logic MUST reside only in service classes
  - Controllers MUST only handle HTTP concerns (routing, request parsing, response mapping)
- **Commit convention**: Conventional Commits — prefixes: `feat`, `fix`, `docs`, `refactor`, `test`
- **Design principles**: SOLID, DRY, KISS, YAGNI — applied in that order of precedence when
  tradeoffs arise
- **Validation**: Fail-fast — validate all inputs at system boundaries before any business
  logic executes

## Performance & Operational Constraints

- API p95 response time MUST be **< 200ms**
- Rate limiting: **100 requests/minute per user** enforced via Redis (token bucket or sliding window)
- Business limits enforced server-side:
  - Maximum **50 trades** per user per day
  - Maximum **20 items** per trade
- All list endpoints MUST be paginated — no unbounded result sets returned
- Passwords and sensitive credentials MUST NEVER appear in logs or error responses

## Development Workflow

- Feature work MUST start from an up-to-date `main` branch
- Each feature MUST have a `spec.md` before a `plan.md`, and a `plan.md` before `tasks.md`
- Constitution Check in `plan.md` MUST be completed and signed off before Phase 0 research
- PRs MUST pass all CI checks (lint, type-check, tests, 80% coverage gate) before merge
- PRs that introduce a breaking API change MUST include a migration note in the PR description
- Code review MUST verify compliance with all five Core Principles before approval
- Commits MUST be atomic and reference the task ID (e.g., `T012`) where applicable

## Governance

This constitution supersedes all other development practices and informal agreements. Any
deviation requires a formal amendment:

1. Open a PR that modifies this file with the proposed change and rationale
2. Increment the version according to semantic versioning (see below)
3. Update `LAST_AMENDED_DATE` to the date of merge
4. Propagate any principle changes to all affected templates (plan, spec, tasks)
5. All active feature branches MUST acknowledge the amendment before their next merge

**Versioning policy**:
- MAJOR bump: Removal or backward-incompatible redefinition of a Core Principle
- MINOR bump: New principle or section added, or materially expanded guidance
- PATCH bump: Clarifications, wording fixes, non-semantic refinements

**Compliance review**: Principles MUST be reviewed at the start of each new major feature
or at minimum every 90 days. The runtime development guidance for the AI agent lives in
`CLAUDE.md` at the repository root.

**Version**: 1.1.0 | **Ratified**: 2026-04-22 | **Last Amended**: 2026-04-22
