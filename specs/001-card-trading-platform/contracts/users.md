# API Contract: Users

**Base URL**: `/api/v1/users`
**Auth required**: Bearer JWT (all endpoints)
**Content-Type**: `application/json`

---

## GET /api/v1/users/{userId}

Get a user's public profile.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `userId` | UUID | Target user's identifier |

### Responses

**200 OK**
```json
{
  "id": "uuid",
  "username": "string",
  "email": "string",
  "role": "USER | ADMIN",
  "createdAt": "ISO-8601 datetime"
}
```

> Note: `email` is only returned when the requesting user is the profile owner or an ADMIN.

**404 Not Found**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found",
  "traceId": "uuid"
}
```

---

## PUT /api/v1/users/{userId}

Update a user's profile. Only the profile owner may update their own profile.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `userId` | UUID | Target user's identifier (must match authenticated user) |

### Request

```json
{
  "username": "string (3–20 chars, optional)",
  "email": "string (valid email, optional)"
}
```

### Responses

**200 OK**
```json
{
  "id": "uuid",
  "username": "string",
  "email": "string",
  "role": "USER | ADMIN",
  "updatedAt": "ISO-8601 datetime"
}
```

**400 Bad Request** — validation failure
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": { "username": "must be between 3 and 20 characters" },
  "traceId": "uuid"
}
```

**403 Forbidden** — attempting to update another user's profile
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You are not authorized to update this profile",
  "traceId": "uuid"
}
```

**409 Conflict** — username or email already taken
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Username already taken",
  "traceId": "uuid"
}
```

---

## GET /api/v1/users/{userId}/inventory

Get a user's card inventory (publicly visible to authenticated users).

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `userId` | UUID | Target user's identifier |

### Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Page number (0-indexed) |
| `size` | integer | 20 | Page size (max 100) |

### Responses

**200 OK**
```json
{
  "content": [
    {
      "cardId": "uuid",
      "cardName": "string",
      "rarity": "COMMON | RARE | EPIC | LEGENDARY",
      "quantity": "integer",
      "acquiredAt": "ISO-8601 datetime",
      "acquiredFrom": "SYSTEM | TRADE | PURCHASE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": "integer",
  "totalPages": "integer"
}
```

---

## GET /api/v1/users/{userId}/trades

Get a user's trade history. Only the profile owner or an ADMIN may access this.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `userId` | UUID | Target user's identifier |

### Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Page number (0-indexed) |
| `size` | integer | 20 | Page size (max 100) |
| `status` | string | (all) | Filter by status: `PENDING`, `ACCEPTED`, `REJECTED`, `COMPLETED`, `CANCELLED`, `FAILED` |

### Responses

**200 OK**
```json
{
  "content": [
    {
      "id": "uuid",
      "offererId": "uuid",
      "offererUsername": "string",
      "receiverId": "uuid",
      "receiverUsername": "string",
      "status": "string",
      "offeredCards": [
        { "cardId": "uuid", "cardName": "string", "quantity": "integer" }
      ],
      "requestedCards": [
        { "cardId": "uuid", "cardName": "string", "quantity": "integer" }
      ],
      "createdAt": "ISO-8601 datetime",
      "updatedAt": "ISO-8601 datetime"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": "integer",
  "totalPages": "integer"
}
```

**403 Forbidden** — non-owner, non-admin access attempt
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You are not authorized to view this user's trades",
  "traceId": "uuid"
}
```
