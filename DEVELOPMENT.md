# Development Guide

How to build and run the Card Trading Platform backend on your local machine.

## Prerequisites

| Tool | Version | Check command |
|------|---------|---------------|
| JDK | 17+ | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Docker | 24+ | `docker --version` |
| Docker Compose | 2.x | `docker compose version` |

## 1. Clone and enter the project

```bash
git clone <repository-url>
cd Backend
```

## 2. Start infrastructure services

The project uses PostgreSQL, Kafka, and Redis. All three run via Docker Compose:

```bash
docker compose -f docker/docker-compose.yml up -d
```

This starts:

| Service | Port | Credentials |
|---------|------|-------------|
| PostgreSQL 15 | 5432 | `cardtrading` / `cardtrading` (db: `cardtrading`) |
| Kafka | 9092 | — |
| Zookeeper | 2181 | — |
| Redis 7 | 6379 | — |

Verify all services are healthy:

```bash
docker compose -f docker/docker-compose.yml ps
```

## 3. Configure environment

The application uses environment variables for secrets. For local development, the defaults in `application-dev.yml` point to the Docker services, so **no extra configuration is needed** to run locally.

If you need to override any values, set these environment variables:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/cardtrading
export DB_USERNAME=cardtrading
export DB_PASSWORD=cardtrading
export JWT_SECRET=your-secret-key-at-least-256-bits-long-for-hmac-sha256
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

**Email** (optional for local dev — email sending will fail silently without a real SMTP server):

```bash
export MAIL_HOST=smtp.example.com
export MAIL_PORT=587
export MAIL_USERNAME=your-email
export MAIL_PASSWORD=your-password
```

## 4. Build the project

```bash
mvn clean install -DskipTests
```

## 5. Run the application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The API starts at: **http://localhost:8080**

Useful URLs:

| URL | Description |
|-----|-------------|
| http://localhost:8080/swagger-ui.html | Swagger UI (interactive API docs) |
| http://localhost:8080/api-docs | OpenAPI JSON spec |
| http://localhost:8080/actuator/health | Health check |

## 6. Create your first admin user

```bash
# 1. Register a regular user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","email":"admin@example.com","password":"Admin123!"}'

# 2. Promote to ADMIN via database
docker exec -it $(docker ps -qf "name=postgres") \
  psql -U cardtrading -d cardtrading \
  -c "UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.com';"

# 3. Login to get your token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin123!"}'
```

The login response contains `accessToken` and `refreshToken`. Use the access token in subsequent requests:

```bash
curl http://localhost:8080/api/v1/admin/stats \
  -H "Authorization: Bearer <your-access-token>"
```

## 7. Run tests

```bash
# Full test suite (unit + integration with Testcontainers)
mvn verify

# Unit tests only (no Docker required beyond build)
mvn test

# Generate coverage report
mvn verify jacoco:report
# Open: target/site/jacoco/index.html
```

**Note**: Integration tests use [Testcontainers](https://testcontainers.com/) which automatically starts disposable Docker containers for PostgreSQL, Kafka, and Redis. Docker must be running.

Coverage gate: **80% minimum line coverage** enforced by Jacoco.

## 8. Database migrations

Flyway migrations run automatically on application startup. The migration files are in:

```
src/main/resources/db/migration/
  V1__create_users.sql
  V2__create_cards.sql
  V3__create_user_cards.sql
  V4__create_trades.sql
  V5__create_trade_items.sql
```

To run migrations manually:

```bash
mvn flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/cardtrading \
  -Dflyway.user=cardtrading \
  -Dflyway.password=cardtrading
```

To repair after a failed migration:

```bash
mvn flyway:repair -Dflyway.url=jdbc:postgresql://localhost:5432/cardtrading \
  -Dflyway.user=cardtrading -Dflyway.password=cardtrading
```

## 9. Kafka topics

Topics are auto-created on startup. To inspect them:

```bash
docker exec -it $(docker ps -qf "name=kafka") \
  kafka-topics --bootstrap-server localhost:9092 --list
```

Topics used:

| Topic | Producer | Consumer |
|-------|----------|----------|
| `trading.user.registered` | AuthService | NotificationConsumer |
| `trading.trade.created` | TradeService | NotificationConsumer |
| `trading.trade.accepted` | TradeService | TradeInventoryConsumer, NotificationConsumer |
| `trading.trade.rejected` | TradeService | NotificationConsumer |
| `trading.trade.completed` | TradeInventoryConsumer | NotificationConsumer |
| `trading.trade.cancelled` | TradeService | NotificationConsumer |

Failed events are routed to `*.DLT` (Dead Letter Topics) after 3 retries with exponential backoff.

## 10. Stopping everything

```bash
# Stop the Spring Boot application: Ctrl+C

# Stop infrastructure
docker compose -f docker/docker-compose.yml down

# Stop and remove volumes (full reset)
docker compose -f docker/docker-compose.yml down -v
```

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `Connection refused` on port 5432/9092/6379 | Run `docker compose -f docker/docker-compose.yml up -d` |
| Flyway checksum mismatch | Run `mvn flyway:repair` then `mvn flyway:migrate` |
| 401 on all authenticated requests | Verify `JWT_SECRET` matches between when the token was issued and the current config |
| Kafka consumer not processing events | Check `docker logs <kafka-container>` and consumer group offsets |
| Tests fail with Docker errors | Ensure Docker daemon is running and your user has Docker permissions |
| `mvn verify` fails on coverage | Run `mvn verify jacoco:report` and check `target/site/jacoco/index.html` for uncovered code |
