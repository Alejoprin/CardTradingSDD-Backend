# Card Trading Platform - Backend API

A REST API backend for a collectible card trading platform where users can register, build their card inventory, create trade offers with other users, and manage their collection — all powered by asynchronous event processing.

## What does this platform do?

### For Traders (Users)

- **Register and authenticate** with secure JWT-based sessions
- **Browse the card catalog** — search by name, filter by rarity (Common, Rare, Epic, Legendary), card type (Monster, Spell, Trap), or edition
- **Manage your inventory** — view your card collection with quantities and acquisition history
- **Create trade offers** — propose a trade by selecting cards you want to give and cards you want to receive from another user
- **Respond to trades** — accept, reject, or cancel pending trade offers
- **Receive email notifications** when trade offers are created, accepted, rejected, or completed
- **Password reset** via email with secure time-limited tokens

### For Administrators

- **Manage the card catalog** — add, update, or remove cards from the platform
- **Moderate users** — ban or unban accounts with reason tracking
- **Monitor the platform** — view all trades, all users, and aggregate statistics (total users, cards, trades by status)

## Core Concepts

### Trade Flow

1. **User A** creates a trade offer specifying cards to give and cards to request from **User B**
2. **User B** receives an email notification about the new offer
3. **User B** can **accept**, **reject**, or let the offer **expire** (auto-cancels after 7 days)
4. If accepted, the card exchange is processed automatically — cards move between inventories
5. Both users are notified of the outcome

### Business Rules

- Users cannot trade with themselves
- Both parties must own the cards they're offering at the time of trade creation
- Maximum 20 card items per trade
- Maximum 50 trade offers per user per day
- Login rate limiting: 5 attempts per 15 minutes per email
- API rate limiting: 100 requests per minute per authenticated user

## API Overview

| Endpoint Group | Base Path | Description |
|---------------|-----------|-------------|
| **Auth** | `/api/v1/auth` | Register, login, token refresh, logout, password reset |
| **Users** | `/api/v1/users` | View/update profiles, view inventories |
| **Cards** | `/api/v1/cards` | Browse, search, and filter the card catalog |
| **Trades** | `/api/v1/trades` | Create, list, accept, reject, and cancel trades |
| **Admin** | `/api/v1/admin` | User management, trade oversight, platform stats |

Full interactive API documentation is available via **Swagger UI** at `/swagger-ui.html` when the application is running.

## Authentication

All endpoints (except registration, login, and password reset) require a valid JWT Bearer token in the `Authorization` header:

```
Authorization: Bearer <access_token>
```

- **Access tokens** expire after 1 hour
- **Refresh tokens** expire after 7 days — use `POST /api/v1/auth/refresh` to get a new access token

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Runtime | Java 17, Spring Boot 3.2 |
| Database | PostgreSQL 15 |
| Caching | Redis 7 |
| Messaging | Apache Kafka |
| Auth | JWT (JJWT), BCrypt |
| Email | Spring Mail + Thymeleaf templates |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Migrations | Flyway |

## Project Structure

```
src/main/java/com/cardtrading/
  auth/          Registration, login, JWT, user profiles
  card/          Card catalog browsing and admin CRUD
  inventory/     User card collection management
  trade/         Trade creation, lifecycle, expiry
  admin/         Admin-only endpoints and stats
  event/         Kafka producers, consumers, event models
  shared/        Security, config, exception handling, filters
```

## Getting Started

See [DEVELOPMENT.md](DEVELOPMENT.md) for instructions on building and running the project locally.

## License

This project is proprietary. All rights reserved.
