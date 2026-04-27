# Developer Quickstart: Card Trading Platform

**Branch**: `001-card-trading-platform`
**Date**: 2026-04-23

---

## Prerequisites

- Java 17 (JDK)
- Maven 3.9+
- Docker & Docker Compose

Verify:
```bash
java -version     # openjdk 17.x
mvn -version      # Apache Maven 3.9.x
docker --version  # Docker 24+
```

---

## 1. Start Infrastructure

From the repository root:

```bash
docker compose -f docker/docker-compose.yml up -d
```

This starts:
- **PostgreSQL 15** on port `5432` (database: `cardtrading`, user: `cardtrading`, password: `cardtrading`)
- **Kafka** on port `9092` (with Zookeeper on `2181`)
- **Redis 7** on port `6379`

Verify everything is healthy:
```bash
docker compose -f docker/docker-compose.yml ps
```

---

## 2. Configure the Application

Copy the dev config template and set environment variables:

```bash
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
```

Required environment variables (or set in `application-dev.yml`):

| Variable | Example Value | Description |
|----------|---------------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/cardtrading` | PostgreSQL connection URL |
| `DB_USERNAME` | `cardtrading` | Database username |
| `DB_PASSWORD` | `cardtrading` | Database password |
| `JWT_SECRET` | `<256-bit base64 secret>` | HMAC-SHA256 signing key |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `MAIL_HOST` | `smtp.example.com` | SMTP server host |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | `noreply@example.com` | SMTP auth username |
| `MAIL_PASSWORD` | `<password>` | SMTP auth password |

> **Never commit secrets.** All sensitive values must be supplied via environment variables.

---

## 3. Run Database Migrations

Flyway runs automatically on application startup. To run manually:

```bash
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/cardtrading \
  -Dflyway.user=cardtrading -Dflyway.password=cardtrading
```

---

## 4. Run the Application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The API is available at: `http://localhost:8080/api/v1`
Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 5. Run Tests

```bash
# All tests (unit + integration — Testcontainers manages its own Docker containers)
mvn verify

# Unit tests only (no Docker required)
mvn test -Dgroups="unit"

# Integration tests only
mvn verify -Dgroups="integration"

# With coverage report
mvn verify jacoco:report
# Report at: target/site/jacoco/index.html
```

Coverage gate: **80% minimum** (enforced by Jacoco in Maven build).

---

## 6. Kafka Topics

Topics are auto-created on application startup via `KafkaConfig`. To inspect them:

```bash
docker exec -it <kafka-container-name> kafka-topics.sh \
  --bootstrap-server localhost:9092 --list
```

Key topics:
- `trading.trade.created`
- `trading.trade.accepted`
- `trading.trade.completed`
- `trading.trade.cancelled`
- `trading.trade.rejected`
- `trading.user.registered`
- `*.DLT` (dead letter topics)

---

## 7. Seed Admin User

On first startup, create an admin user via the registration endpoint and then manually update the role in the database:

```bash
# Register a user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","email":"admin@example.com","password":"Admin123!"}'

# Promote to ADMIN (in psql)
docker exec -it <postgres-container> psql -U cardtrading -d cardtrading \
  -c "UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.com';"
```

---

## 8. Health Checks

```bash
curl http://localhost:8080/health
curl http://localhost:8080/ready
```

---

## Key Module Locations

| Module | Package | Purpose |
|--------|---------|---------|
| `auth` | `com.cardtrading.auth` | Registration, login, JWT, password reset |
| `card` | `com.cardtrading.card` | Card catalog CRUD + Redis cache |
| `trade` | `com.cardtrading.trade` | Trade creation, acceptance, cancellation |
| `inventory` | `com.cardtrading.inventory` | UserCard quantity management |
| `event` | `com.cardtrading.event` | Kafka producers + consumers |
| `admin` | `com.cardtrading.admin` | Admin-only endpoints |
| `shared` | `com.cardtrading.shared` | Security config, exception handling, filters |

---

## Common Issues

| Problem | Solution |
|---------|---------|
| `Connection refused` on port 5432 | Docker Compose not running — `docker compose up -d` |
| Flyway migration checksum error | Schema out of sync — run `mvn flyway:repair` then retry |
| Kafka consumer not receiving events | Check consumer group offset: `kafka-consumer-groups.sh --describe` |
| 401 on all requests | JWT secret mismatch between startup config and test — verify `JWT_SECRET` env var |
| Coverage gate fails | Run `mvn verify jacoco:report` and check uncovered branches |
