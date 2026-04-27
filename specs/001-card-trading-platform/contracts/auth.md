# API Contract: Authentication

**Base URL**: `/api/v1/auth`
**Auth required**: None (all endpoints are public under `/api/v1/auth/**`)
**Content-Type**: `application/json`

---

## POST /api/v1/auth/register

Register a new user account.

### Request

```json
{
  "username": "string (3–20 chars, alphanumeric + underscore)",
  "email": "string (valid email)",
  "password": "string (min 8 chars, ≥1 uppercase, ≥1 lowercase, ≥1 digit)"
}
```

### Responses

**201 Created**
```json
{
  "id": "uuid",
  "username": "string",
  "email": "string",
  "role": "USER",
  "createdAt": "ISO-8601 datetime"
}
```

**400 Bad Request** — validation failure
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "username": "must be between 3 and 20 characters",
    "password": "must contain at least one uppercase letter, one lowercase letter, and one digit"
  },
  "traceId": "uuid"
}
```

**409 Conflict** — username or email already taken
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Email already in use",
  "traceId": "uuid"
}
```

---

## POST /api/v1/auth/login

Authenticate with email and password; returns JWT tokens.

### Request

```json
{
  "email": "string",
  "password": "string"
}
```

### Responses

**200 OK**
```json
{
  "accessToken": "string (JWT, expires in 1 hour)",
  "refreshToken": "string (JWT, expires in 7 days)",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**401 Unauthorized** — bad credentials
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password",
  "traceId": "uuid"
}
```

**429 Too Many Requests** — rate limit exceeded (5 attempts / 15 min)
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Too many login attempts. Try again in 14 minutes.",
  "traceId": "uuid"
}
```

---

## POST /api/v1/auth/refresh

Exchange a valid refresh token for a new access token.

### Request

```json
{
  "refreshToken": "string"
}
```

### Responses

**200 OK**
```json
{
  "accessToken": "string (JWT)",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**401 Unauthorized** — token invalid or expired
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Refresh token is invalid or expired",
  "traceId": "uuid"
}
```

---

## POST /api/v1/auth/logout

Invalidate the current session (blacklists the refresh token).

### Request

```json
{
  "refreshToken": "string"
}
```

### Responses

**200 OK**
```json
{
  "message": "Successfully logged out"
}
```

---

## POST /api/v1/auth/password/reset

Initiate a password reset. Sends a reset link to the provided email.

### Request

```json
{
  "email": "string"
}
```

### Responses

**200 OK** — always returns 200 (no email enumeration)
```json
{
  "message": "If an account with that email exists, a reset link has been sent."
}
```

---

## POST /api/v1/auth/password/reset/confirm

Complete the password reset using the token from the email link.

### Request

```json
{
  "token": "string (UUID reset token)",
  "newPassword": "string (min 8 chars, ≥1 uppercase, ≥1 lowercase, ≥1 digit)"
}
```

### Responses

**200 OK**
```json
{
  "message": "Password successfully reset."
}
```

**400 Bad Request** — token invalid, expired, or already used
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Reset token is invalid or has expired",
  "traceId": "uuid"
}
```

---

## Error Response Format (All Endpoints)

```json
{
  "status": "integer (HTTP status code)",
  "error": "string (HTTP status reason)",
  "message": "string (human-readable description)",
  "details": "object | null (field-level validation errors if applicable)",
  "traceId": "string (correlation ID from request context)"
}
```
