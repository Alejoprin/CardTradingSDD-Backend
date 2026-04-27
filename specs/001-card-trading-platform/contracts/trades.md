# API Contract: Trades

**Base URL**: `/api/v1/trades`
**Auth required**: Bearer JWT (all endpoints)
**Content-Type**: `application/json`

---

## POST /api/v1/trades

Create a new trade offer.

### Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Idempotency-Key` | Recommended | UUID to prevent duplicate submissions on retry |

### Request

```json
{
  "receiverId": "uuid (target user)",
  "offeredCards": [
    { "cardId": "uuid", "quantity": "integer (≥ 1)" }
  ],
  "requestedCards": [
    { "cardId": "uuid", "quantity": "integer (≥ 1)" }
  ]
}
```

**Constraints**:
- `offeredCards` + `requestedCards` total items ≤ 20
- `offeredCards` must not be empty
- `requestedCards` must not be empty
- Caller (`offererId`) ≠ `receiverId`

### Responses

**201 Created**
```json
{
  "id": "uuid",
  "offererId": "uuid",
  "offererUsername": "string",
  "receiverId": "uuid",
  "receiverUsername": "string",
  "status": "PENDING",
  "offeredCards": [
    { "cardId": "uuid", "cardName": "string", "quantity": "integer" }
  ],
  "requestedCards": [
    { "cardId": "uuid", "cardName": "string", "quantity": "integer" }
  ],
  "createdAt": "ISO-8601 datetime"
}
```

**400 Bad Request** — validation or business rule failure
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Insufficient quantity of card 'Dragon Shield' in your inventory",
  "traceId": "uuid"
}
```

**409 Conflict** — idempotency key already used (returns original response)
```json
{
  "id": "uuid (original trade id)",
  "status": "PENDING",
  "message": "Duplicate request — returning original trade"
}
```

**422 Unprocessable Entity** — business rule violations
```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "You cannot trade with yourself",
  "traceId": "uuid"
}
```
> Also used for: daily trade limit exceeded; receiver does not own requested cards.

---

## GET /api/v1/trades

List the authenticated user's trades (both sent and received).

### Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Page number (0-indexed) |
| `size` | integer | 20 | Page size (max 100) |
| `status` | string | (all) | Filter: `PENDING`, `ACCEPTED`, `REJECTED`, `COMPLETED`, `CANCELLED`, `FAILED` |
| `direction` | string | (all) | Filter: `SENT` (as offerer), `RECEIVED` (as receiver) |

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

---

## GET /api/v1/trades/{tradeId}

Get details of a specific trade. Only the offerer, receiver, or an ADMIN may access it.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `tradeId` | UUID | Trade identifier |

### Responses

**200 OK** — same shape as a single element from `GET /trades` content array, plus:
```json
{
  "acceptedAt": "ISO-8601 datetime | null",
  "completedAt": "ISO-8601 datetime | null"
}
```

**403 Forbidden** — caller is neither offerer, receiver, nor admin

**404 Not Found** — trade does not exist

---

## PUT /api/v1/trades/{tradeId}/accept

Accept a pending trade offer. Only the **receiver** may call this.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `tradeId` | UUID | Trade identifier |

### Request Body

None.

### Responses

**200 OK**
```json
{
  "id": "uuid",
  "status": "ACCEPTED",
  "acceptedAt": "ISO-8601 datetime",
  "message": "Trade accepted. Inventory update is being processed."
}
```

**403 Forbidden** — caller is not the receiver

**404 Not Found** — trade does not exist

**409 Conflict** — trade is no longer PENDING
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Trade cannot be accepted: current status is CANCELLED",
  "traceId": "uuid"
}
```

---

## PUT /api/v1/trades/{tradeId}/reject

Reject a pending trade offer. Only the **receiver** may call this.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `tradeId` | UUID | Trade identifier |

### Request Body (optional)

```json
{
  "reason": "string (optional, max 500 chars)"
}
```

### Responses

**200 OK**
```json
{
  "id": "uuid",
  "status": "REJECTED",
  "updatedAt": "ISO-8601 datetime"
}
```

**403 Forbidden** — caller is not the receiver

**409 Conflict** — trade is no longer PENDING

---

## DELETE /api/v1/trades/{tradeId}

Cancel a pending trade offer. Only the **offerer** may call this, and only while status is `PENDING`.

### Path Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `tradeId` | UUID | Trade identifier |

### Responses

**200 OK**
```json
{
  "id": "uuid",
  "status": "CANCELLED",
  "updatedAt": "ISO-8601 datetime"
}
```

**403 Forbidden** — caller is not the offerer

**409 Conflict** — trade is no longer PENDING
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Trade cannot be cancelled: current status is ACCEPTED",
  "traceId": "uuid"
}
```
