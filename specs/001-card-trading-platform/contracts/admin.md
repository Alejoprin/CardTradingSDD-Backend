# API Contract: Admin

**Base URL**: `/api/v1/admin`
**Auth required**: Bearer JWT with `ROLE_ADMIN` (all endpoints)
**Content-Type**: `application/json`

---

## GET /api/v1/admin/users

List all registered users (including soft-deleted if queried).

### Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Page number (0-indexed) |
| `size` | integer | 20 | Page size (max 100) |
| `search` | string | — | Filter by username or email (partial match) |
| `banned` | boolean | — | Filter by ban status |

### Responses

**200 OK**
```json
{
  "content": [
    {
      "id": "uuid",
      "username": "string",
      "email": "string",
      "role": "USER | ADMIN",
      "isBanned": "boolean",
      "createdAt": "ISO-8601 datetime",
      "deletedAt": "ISO-8601 datetime | null"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": "integer",
  "totalPages": "integer"
}
```

**403 Forbidden** — caller is not ADMIN

---

## GET /api/v1/admin/trades

List all trades platform-wide.

### Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Page number (0-indexed) |
| `size` | integer | 20 | Page size (max 100) |
| `status` | string | (all) | Filter by status |
| `userId` | UUID | — | Filter trades involving a specific user |

### Responses

**200 OK** — same pagination shape as `GET /api/v1/trades`, content includes all users' trades.

**403 Forbidden** — caller is not ADMIN

---

## PUT /api/v1/admin/users/{userId}/ban

Ban or unban a user account.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `userId` | UUID | Target user's identifier |

### Request

```json
{
  "banned": "boolean (true to ban, false to unban)",
  "reason": "string (optional, max 500 chars)"
}
```

### Responses

**200 OK**
```json
{
  "id": "uuid",
  "username": "string",
  "isBanned": "boolean",
  "updatedAt": "ISO-8601 datetime"
}
```

**404 Not Found** — user does not exist

**403 Forbidden** — caller is not ADMIN

**409 Conflict** — attempting to ban a user who is already banned (or unban one who is not)
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "User is already banned",
  "traceId": "uuid"
}
```

---

## GET /api/v1/admin/stats

Get aggregate platform statistics.

### Responses

**200 OK**
```json
{
  "totalUsers": "integer",
  "activeUsers": "integer",
  "bannedUsers": "integer",
  "totalCards": "integer",
  "totalTrades": "integer",
  "tradesByStatus": {
    "PENDING": "integer",
    "ACCEPTED": "integer",
    "COMPLETED": "integer",
    "REJECTED": "integer",
    "CANCELLED": "integer",
    "FAILED": "integer"
  },
  "generatedAt": "ISO-8601 datetime"
}
```

**403 Forbidden** — caller is not ADMIN
