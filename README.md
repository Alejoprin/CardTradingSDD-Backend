# Card Trading Platform — Backend API

Base URL: `http://localhost:8080`

---

## Autenticación

El sistema usa **JWT (Bearer token)** para autenticación. El token de acceso va en el header `Authorization`. El refresh token se gestiona automáticamente mediante una **cookie HttpOnly** llamada `refresh_token`.

### Header requerido en todas las peticiones autenticadas
```
Authorization: Bearer <accessToken>
```

### Flujo de autenticación

1. El usuario hace login → recibe `accessToken` (1 hora de vida)
2. La cookie `refresh_token` se establece automáticamente (7 días)
3. Cuando el `accessToken` expire, llamar a `/api/v1/auth/refresh` para obtener uno nuevo

---

## Endpoints

### Auth — `/api/v1/auth`

#### `POST /api/v1/auth/register`
Registro de nuevo usuario. No requiere autenticación.

**Body:**
```json
{
  "username": "string (3-20 chars, solo letras, números y _)",
  "email": "string (email válido)",
  "password": "string (mín. 8 chars, debe tener mayúscula, minúscula y número)"
}
```

**Respuesta 201:**
```json
{
  "id": "uuid",
  "username": "string",
  "email": "string",
  "role": "USER",
  "createdAt": "2026-04-29T10:00:00"
}
```

---

#### `POST /api/v1/auth/login`
Login. No requiere autenticación.

**Body:**
```json
{
  "email": "string",
  "password": "string"
}
```

**Respuesta 200:**
```json
{
  "accessToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```
> La cookie `refresh_token` se establece automáticamente en la respuesta.

---

#### `POST /api/v1/auth/refresh`
Renueva el access token usando la cookie `refresh_token`. No requiere Authorization header.

**Respuesta 200:** igual que login

---

#### `POST /api/v1/auth/logout`
Cierra sesión e invalida el refresh token.

**Respuesta 200:**
```json
{ "message": "Successfully logged out" }
```

---

#### `POST /api/v1/auth/password/reset`
Solicita email de recuperación de contraseña.

**Body:**
```json
{ "email": "string" }
```

**Respuesta 200:**
```json
{ "message": "If an account with that email exists, a reset link has been sent." }
```

---

#### `POST /api/v1/auth/password/reset/confirm`
Confirma el reset con el token recibido por email.

**Body:**
```json
{
  "token": "string",
  "newPassword": "string"
}
```

---

### Usuarios — `/api/v1/users`

#### `GET /api/v1/users/{userId}`
Obtiene el perfil de un usuario. Requiere autenticación.

**Respuesta 200:**
```json
{
  "id": "uuid",
  "username": "string",
  "email": "string",
  "role": "USER | ADMIN",
  "createdAt": "2026-04-29T10:00:00"
}
```

---

#### `PUT /api/v1/users/{userId}`
Actualiza el perfil. Solo puede editar su propio perfil.

**Body (todos opcionales):**
```json
{
  "username": "string",
  "email": "string"
}
```

**Respuesta 200:** igual que GET usuario

---

### Inventario — `/api/v1/users/{userId}/inventory`

#### `GET /api/v1/users/{userId}/inventory`
Obtiene las cartas del inventario de un usuario. Requiere autenticación.

**Query params:**
| Param | Tipo | Default | Descripción |
|-------|------|---------|-------------|
| `page` | int | 0 | Número de página |
| `size` | int | 20 | Tamaño de página (máx. 100) |

**Respuesta 200 (paginada):**
```json
{
  "content": [
    {
      "userCardId": "uuid",
      "cardId": "uuid",
      "cardName": "Pikachu",
      "cardNumber": "058/102",
      "rarity": "COMMON",
      "imageUrl": "string | null",
      "imageSmallUrl": "string | null",
      "marketPrice": 5.99,
      "setName": "Base Set",
      "gameName": "Pokémon TCG",
      "quantity": 2,
      "condition": "NEAR_MINT",
      "forTrade": false,
      "forSale": false,
      "notes": "string | null",
      "acquiredAt": "2026-04-29T10:00:00"
    }
  ],
  "page": { "number": 0, "size": 20, "totalElements": 5, "totalPages": 1 }
}
```

---

#### `POST /api/v1/users/{userId}/inventory`
Añade una carta del catálogo al inventario del usuario. Solo el propio usuario puede añadir a su inventario.

**Body:**
```json
{
  "cardId": "uuid",
  "quantity": 1,
  "condition": "NEAR_MINT",
  "notes": "string | null"
}
```

**Respuesta 201:** mismo objeto que cada item del GET inventario

---

### Cartas (Catálogo) — `/api/v1/cards`

El catálogo está organizado en: **Juegos → Sets → Cartas**

Juegos disponibles (pre-cargados): `Pokémon TCG`, `Magic: The Gathering`, `Yu-Gi-Oh!`

#### `GET /api/v1/cards`
Lista el catálogo de cartas. Público, no requiere autenticación.

**Query params:**
| Param | Tipo | Descripción |
|-------|------|-------------|
| `search` | string | Búsqueda por nombre |
| `rarity` | string | Filtrar por rareza |
| `setId` | uuid | Filtrar por set |
| `page` | int | Default 0 |
| `size` | int | Default 20, máx 100 |

**Respuesta 200 (paginada):**
```json
{
  "content": [
    {
      "id": "uuid",
      "setId": "uuid",
      "setName": "Base Set",
      "gameName": "Pokémon TCG",
      "name": "Pikachu",
      "cardNumber": "058/102",
      "rarity": "COMMON",
      "imageUrl": "string | null",
      "imageSmallUrl": "string | null",
      "marketPrice": 5.99
    }
  ],
  "page": { "number": 0, "size": 20, "totalElements": 102, "totalPages": 6 }
}
```

---

#### `GET /api/v1/cards/{cardId}`
Detalle de una carta. Público.

**Respuesta 200:**
```json
{
  "id": "uuid",
  "setId": "uuid",
  "setName": "Base Set",
  "setCode": "BS",
  "gameId": "uuid",
  "gameName": "Pokémon TCG",
  "name": "Pikachu",
  "cardNumber": "058/102",
  "rarity": "COMMON",
  "attributes": "{\"hp\": 40, \"type\": \"Lightning\"}",
  "imageUrl": "string | null",
  "imageSmallUrl": "string | null",
  "marketPrice": 5.99,
  "lastPriceUpdate": "2026-04-29T10:00:00",
  "createdAt": "2026-04-29T10:00:00",
  "updatedAt": "2026-04-29T10:00:00"
}
```

---

#### `POST /api/v1/cards` — **ADMIN**
Crea una carta en el catálogo.

**Sin imagen:**
```
Content-Type: application/json

{
  "setId": "uuid",
  "name": "Pikachu",
  "cardNumber": "058/102",
  "rarity": "COMMON",
  "attributes": "{\"hp\": 40}",
  "marketPrice": 5.99
}
```

**Con imagen:**
```
Content-Type: multipart/form-data

Part "data"  → Blob con Content-Type: application/json → los mismos campos del JSON
Part "image" → Archivo (JPEG, PNG o WebP, máx 5 MB)
```

**Respuesta 201:** `CardDetailResponse`

---

#### `PUT /api/v1/cards/{cardId}` — **ADMIN**
Actualiza una carta. Mismo formato que POST. Todos los campos son opcionales.

**Respuesta 200:** `CardDetailResponse`

---

#### `DELETE /api/v1/cards/{cardId}` — **ADMIN**
Elimina una carta del catálogo (hard delete, también elimina los user_cards asociados).

**Respuesta 204** (sin body)

---

### Trades — `/api/v1/trades`

#### Concepto importante
Los trades intercambian **user_cards** (entradas del inventario de cada usuario), no cartas del catálogo directamente. Cada `userCardId` es una carta específica en el inventario de alguien, con su condición y notas.

**Flujo típico:**
1. Usuario A consulta el inventario de Usuario B para ver sus `userCardId`s
2. Usuario A propone un trade: ofrece sus propios `userCardId`s, pide los de B
3. Usuario B acepta/rechaza
4. Si acepta, las cartas se transfieren automáticamente entre inventarios

---

#### `POST /api/v1/trades`
Propone un trade. Requiere autenticación.

**Body:**
```json
{
  "receiverId": "uuid",
  "offeredCards": [
    { "userCardId": "uuid" }
  ],
  "requestedCards": [
    { "userCardId": "uuid" }
  ],
  "proposerNotes": "string | null"
}
```

**Respuesta 201:**
```json
{
  "id": "uuid",
  "proposerId": "uuid",
  "proposerUsername": "string",
  "receiverId": "uuid",
  "receiverUsername": "string",
  "status": "PENDING",
  "proposerNotes": "string | null",
  "receiverNotes": "string | null",
  "items": [
    {
      "userCardId": "uuid",
      "cardId": "uuid",
      "cardName": "Pikachu",
      "rarity": "COMMON",
      "imageUrl": "string | null",
      "fromUserId": "uuid",
      "fromUsername": "string"
    }
  ],
  "proposedAt": "2026-04-29T10:00:00",
  "respondedAt": null,
  "completedAt": null,
  "createdAt": "2026-04-29T10:00:00",
  "updatedAt": "2026-04-29T10:00:00",
  "message": null
}
```

> En `items`, los que tienen `fromUserId = proposerId` son las cartas que ofrece el proposer. Los que tienen `fromUserId = receiverId` son las cartas que solicita.

---

#### `GET /api/v1/trades`
Lista los trades del usuario autenticado (como proposer o receiver).

**Query params:**
| Param | Tipo | Descripción |
|-------|------|-------------|
| `status` | string | `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED`, `COMPLETED` |
| `page` | int | Default 0 |
| `size` | int | Default 20 |

**Respuesta 200 (paginada):** lista de `TradeResponse`

---

#### `GET /api/v1/trades/{tradeId}`
Detalle de un trade. Solo accesible por el proposer o el receiver.

**Respuesta 200:** `TradeResponse`

---

#### `PUT /api/v1/trades/{tradeId}/accept`
El receiver acepta el trade. Las cartas se intercambian automáticamente en background.

**Respuesta 200:** `TradeResponse` con `status: "ACCEPTED"`

---

#### `PUT /api/v1/trades/{tradeId}/reject`
El receiver rechaza el trade.

**Body (opcional):**
```json
{ "reason": "string" }
```

**Respuesta 200:** `TradeResponse` con `status: "REJECTED"`

---

#### `DELETE /api/v1/trades/{tradeId}`
El proposer cancela el trade. Solo si está en estado `PENDING`.

**Respuesta 200:** `TradeResponse` con `status: "CANCELLED"`

---

### Admin — `/api/v1/admin` — **Solo ADMIN**

#### `GET /api/v1/admin/users`
Lista todos los usuarios de la plataforma.

**Query params:** `page`, `size`

**Respuesta 200 (paginada):**
```json
{
  "content": [
    {
      "id": "uuid",
      "username": "string",
      "email": "string",
      "role": "USER | ADMIN",
      "isBanned": false,
      "createdAt": "2026-04-29T10:00:00",
      "deletedAt": null
    }
  ]
}
```

---

#### `GET /api/v1/admin/trades`
Lista todos los trades de la plataforma.

**Query params:** `status`, `page`, `size`

**Respuesta 200 (paginada):** lista de `TradeResponse`

---

#### `PUT /api/v1/admin/users/{userId}/ban`
Banea o desbanea un usuario.

**Body:**
```json
{
  "banned": true,
  "reason": "string | null"
}
```

**Respuesta 200:**
```json
{
  "id": "uuid",
  "username": "string",
  "isBanned": true,
  "updatedAt": "2026-04-29T10:00:00"
}
```

---

#### `GET /api/v1/admin/stats`
Estadísticas globales de la plataforma.

**Respuesta 200:**
```json
{
  "totalUsers": 150,
  "activeUsers": 148,
  "bannedUsers": 2,
  "totalCards": 500,
  "totalTrades": 320,
  "tradesByStatus": {
    "PENDING": 12,
    "ACCEPTED": 5,
    "REJECTED": 30,
    "CANCELLED": 18,
    "COMPLETED": 255
  },
  "generatedAt": "2026-04-29T10:00:00"
}
```

---

## Respuestas de error

Todos los errores siguen este formato:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Descripción del error",
  "details": {
    "campo": "mensaje de validación"
  },
  "traceId": "uuid-para-soporte"
}
```

| Código | Cuándo ocurre |
|--------|---------------|
| `400` | Validación fallida (campos requeridos, formato incorrecto) |
| `401` | Token ausente, expirado o inválido |
| `403` | Sin permisos (ej. usuario normal accede a endpoint ADMIN) |
| `404` | Recurso no encontrado |
| `422` | Regla de negocio violada (ej. "no tienes esa carta en tu inventario") |
| `429` | Demasiadas peticiones (rate limiting) |
| `500` | Error interno del servidor |

---

## Valores de enums

### Rareza (`rarity`)
`COMMON` · `UNCOMMON` · `RARE` · `EPIC` · `LEGENDARY` · `SECRET`

### Condición de carta (`condition`)
`MINT` · `NEAR_MINT` · `EXCELLENT` · `GOOD` · `PLAYED` · `POOR`

### Estado de trade (`status`)
`PENDING` · `ACCEPTED` · `REJECTED` · `CANCELLED` · `COMPLETED`

### Rol de usuario (`role`)
`USER` · `ADMIN`

---

## Estructura de datos (Referencia)

```
card_games          → Pokémon TCG, Magic: The Gathering, Yu-Gi-Oh!
  └── card_sets     → Colecciones de un juego (Base Set, Jungle, etc.)
        └── cards   → Cartas individuales del catálogo (admin las gestiona)

users
  └── user_cards    → Cartas que tiene un usuario en su inventario
        └── trade_items → Cartas involucradas en un trade

trades              → Propuestas de intercambio entre dos usuarios
```

---

## Notas importantes

### Autenticación
- Guardar el `accessToken` en **memoria** o estado de la app (no en `localStorage` por seguridad).
- La cookie `refresh_token` es **HttpOnly** — el browser la envía automáticamente, el frontend no necesita gestionarla.
- Implementar un **interceptor HTTP** que, al recibir un `401`, llame a `/api/v1/auth/refresh` y reintente la petición original con el nuevo token.

### Imágenes
- Las imágenes servidas desde el backend tienen rutas relativas como `/uploads/cards/uuid.jpg`.
- URL completa en desarrollo: `http://localhost:8080/uploads/cards/uuid.jpg`

### Paginación
- Todas las listas devuelven un objeto con `content` (array) y `page` (metadata).
- `page` contiene: `number`, `size`, `totalElements`, `totalPages`.

### Roles
- El JWT contiene el campo `role` en el payload. Decodificarlo para mostrar/ocultar secciones de admin en el frontend.
- Los endpoints `/api/v1/admin/**` y los de crear/editar/eliminar cartas devuelven `403` si el usuario no es ADMIN.

### Trades
- El usuario debe primero ver el inventario del otro usuario (`GET /api/v1/users/{userId}/inventory`) para conocer sus `userCardId`s antes de proponer un trade.
- Las cartas de un trade se identifican por `userCardId`, no por `cardId`.
- Distinguir en la UI quién ofrece qué: los `items` con `fromUserId === proposerId` son las cartas que ofrece el proposer; los que tienen `fromUserId === receiverId` son las que pide.
- Los trades en `PENDING` expiran automáticamente a los 7 días.

### Rate limiting
- La API tiene límite de peticiones por IP. Si recibes `429`, espera antes de reintentar.
- Trades: máximo 50 propuestas por día por usuario.
