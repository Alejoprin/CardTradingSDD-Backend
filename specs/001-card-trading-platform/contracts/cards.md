# API Contract: Cards

**Base URL**: `/api/v1/cards`
**Auth required**: Bearer JWT (all endpoints); `POST`, `PUT`, `DELETE` require `ROLE_ADMIN`
**Content-Type**: `application/json`

---

## GET /api/v1/cards

List all cards with pagination, search, and filtering. Results are cached.

### Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Page number (0-indexed) |
| `size` | integer | 20 | Page size (max 100) |
| `search` | string | — | Full-text search on card name |
| `rarity` | string | — | Filter: `COMMON`, `RARE`, `EPIC`, `LEGENDARY` |
| `cardType` | string | — | Filter: `MONSTER`, `SPELL`, `TRAP` |
| `edition` | string | — | Filter by edition string (exact match) |

### Responses

**200 OK**
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "string",
      "rarity": "COMMON | RARE | EPIC | LEGENDARY",
      "cardType": "MONSTER | SPELL | TRAP",
      "edition": "string | null",
      "imageUrl": "string | null"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": "integer",
  "totalPages": "integer"
}
```

---

## GET /api/v1/cards/{cardId}

Get full details for a single card.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `cardId` | UUID | Card identifier |

### Responses

**200 OK**
```json
{
  "id": "uuid",
  "name": "string",
  "description": "string | null",
  "rarity": "COMMON | RARE | EPIC | LEGENDARY",
  "cardType": "MONSTER | SPELL | TRAP",
  "edition": "string | null",
  "imageUrl": "string | null",
  "createdAt": "ISO-8601 datetime",
  "updatedAt": "ISO-8601 datetime"
}
```

**404 Not Found**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Card not found",
  "traceId": "uuid"
}
```

---

## POST /api/v1/cards

Create a new card. **ADMIN only**.

### Request

```json
{
  "name": "string (required, unique)",
  "description": "string (optional)",
  "rarity": "COMMON | RARE | EPIC | LEGENDARY (required)",
  "cardType": "MONSTER | SPELL | TRAP (required)",
  "edition": "string (optional)",
  "imageUrl": "string (optional, valid URL)"
}
```

### Responses

**201 Created**
```json
{
  "id": "uuid",
  "name": "string",
  "description": "string | null",
  "rarity": "string",
  "cardType": "string",
  "edition": "string | null",
  "imageUrl": "string | null",
  "createdAt": "ISO-8601 datetime"
}
```

**400 Bad Request** — validation failure
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": { "name": "must not be blank", "rarity": "must be one of: COMMON, RARE, EPIC, LEGENDARY" },
  "traceId": "uuid"
}
```

**403 Forbidden** — non-admin caller
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Admin access required",
  "traceId": "uuid"
}
```

**409 Conflict** — card name already exists
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "A card with that name already exists",
  "traceId": "uuid"
}
```

---

## PUT /api/v1/cards/{cardId}

Update an existing card. **ADMIN only**. Triggers cache eviction.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `cardId` | UUID | Card identifier |

### Request

```json
{
  "name": "string (optional)",
  "description": "string (optional)",
  "rarity": "COMMON | RARE | EPIC | LEGENDARY (optional)",
  "cardType": "MONSTER | SPELL | TRAP (optional)",
  "edition": "string (optional)",
  "imageUrl": "string (optional)"
}
```

### Responses

**200 OK** — same shape as `GET /cards/{cardId}`

**404 Not Found** — card does not exist

**403 Forbidden** — non-admin caller

---

## DELETE /api/v1/cards/{cardId}

Soft-delete a card from the catalog. **ADMIN only**. Triggers cache eviction.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `cardId` | UUID | Card identifier |

### Responses

**204 No Content** — card soft-deleted successfully

**404 Not Found** — card does not exist

**403 Forbidden** — non-admin caller
