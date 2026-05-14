# Card Trading Platform — API Reference

Base URL: `http://localhost:8080`

---

## Índice

1. [Autenticación](#autenticación)
2. [Rate Limiting](#rate-limiting)
3. [Formato de errores](#formato-de-errores)
4. [Imágenes](#imágenes)
5. [Enums](#enums)
6. [Auth](#auth--apiv1auth)
7. [Usuarios](#usuarios--apiv1users)
8. [Inventario](#inventario--apiv1usersuseridInventory)
9. [Catálogo de cartas](#catálogo-de-cartas--apiv1cards) *(incluye juegos, sets y owners)*
10. [Trades](#trades--apiv1trades)
11. [Admin](#admin--apiv1admin--solo-role-admin)

---

## Autenticación

Todos los endpoints marcados como **🔒 Requiere auth** necesitan el header:

```
Authorization: Bearer <accessToken>
```

El `accessToken` se obtiene en el login y dura **1 hora**.

Cuando expire, llama a `POST /api/v1/auth/refresh` para obtener uno nuevo sin que el usuario tenga que loguearse de nuevo. La cookie `refresh_token` se gestiona automáticamente por el browser (HttpOnly, Secure, SameSite=Strict, path=/api/v1/auth) y dura **7 días**.

> **Importante:** El `accessToken` guárdalo en memoria o en estado de la app (React state, Zustand, etc.), **nunca en localStorage** por seguridad XSS.

---

## Imágenes

Las imágenes se almacenan en **Cloudinary**. Hay dos carpetas:

| Carpeta | Uso |
|---------|-----|
| `cards/` | Imágenes del catálogo y cartas custom del inventario |
| `User_Img/` | Fotos de perfil de usuario |

**Lo que necesita saber el frontend:**

- Los campos `imageUrl` e `imageSmallUrl` que devuelve la API son **URLs públicas directas** de Cloudinary con este formato:
  ```
  https://res.cloudinary.com/{cloud_name}/image/upload/v{version}/cards/uuid.jpg
  ```
- Puedes usarlas directamente en un `<img src={card.imageUrl} />` **sin ningún header de autenticación**. Son públicas.
- `imageSmallUrl` es un campo reservado para una versión reducida de la imagen (thumbnail). Actualmente puede ser `null` — usar `imageUrl` como fallback.
- La **subida** de imágenes pasa siempre por el backend. El frontend envía el archivo como `multipart/form-data` y el backend devuelve la URL ya lista en la respuesta. El frontend no interactúa con Cloudinary directamente.
- Formatos aceptados: **JPEG, PNG, WebP**. Tamaño máximo: **5 MB**.

```jsx
// Ejemplo de uso en React
<img
  src={card.imageUrl ?? '/placeholder-card.png'}
  alt={card.name}
/>
```

---

## Rate Limiting

- **API general:** 100 peticiones por minuto por usuario autenticado. Devuelve `429` si se supera.
- **Login:** Bloqueo temporal tras varios intentos fallidos consecutivos. Devuelve `429`.
- **Propuestas de trade:** Máximo 50 por día por usuario. Devuelve `422`.
- Los endpoints de auth (`/api/v1/auth/**`) están excluidos del rate limit general.

---

## Formato de errores

Todos los errores tienen este formato:

```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Descripción del error",
  "details": {
    "campo": "mensaje de validación"
  },
  "traceId": "uuid-para-soporte"
}
```

> `details` solo aparece en errores de validación (`400`).

| Código | Cuándo ocurre |
|--------|---------------|
| `400` | Campos requeridos faltantes o formato incorrecto |
| `401` | Token ausente, expirado, inválido, o cuenta baneada |
| `403` | Sin permisos (usuario normal accede a endpoint ADMIN) |
| `404` | Recurso no encontrado |
| `422` | Regla de negocio violada (carta no en inventario, trade ya aceptado, etc.) |
| `429` | Demasiadas peticiones |
| `500` | Error interno del servidor |

---

## Enums

### Rareza (`rarity`)
```
COMMON | UNCOMMON | RARE | EPIC | LEGENDARY | SECRET
```

### Condición de carta (`condition`)
```
MINT | NEAR_MINT | EXCELLENT | GOOD | PLAYED | POOR
```

### Estado de trade (`status`)
```
PENDING | ACCEPTED | REJECTED | CANCELLED | COMPLETED
```

### Rol de usuario (`role`)
```
USER | ADMIN
```

---

## Auth — `/api/v1/auth`

> Ningún endpoint de auth requiere token. Están excluidos del rate limit general.

---

### `POST /api/v1/auth/register`

Registra un nuevo usuario. Devuelve `201`.

**Body:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string"
}
```

**Validaciones:**
- `username`: obligatorio, 3–20 caracteres, solo letras, números y `_`
- `email`: obligatorio, formato email válido
- `password`: obligatorio, mínimo 8 caracteres, debe tener al menos una mayúscula, una minúscula y un número

**Respuesta `201`:**
```json
{
  "id": "uuid",
  "username": "string",
  "email": "string",
  "role": "USER",
  "createdAt": "2026-04-30T10:00:00"
}
```

**Errores:**
- `400` — validación fallida (campos inválidos)
- `422` — email o username ya en uso

---

### `POST /api/v1/auth/login`

Inicia sesión. Devuelve el `accessToken` en el body y establece la cookie `refresh_token` automáticamente.

**Body:**
```json
{
  "email": "string",
  "password": "string"
}
```

**Respuesta `200`:**
```json
{
  "accessToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

> La cookie `refresh_token` se establece en la respuesta con `Set-Cookie`. El browser la enviará automáticamente en las llamadas a `/api/v1/auth`.

**Errores:**
- `400` — validación fallida
- `401` — credenciales incorrectas o cuenta baneada
- `429` — demasiados intentos fallidos

---

### `POST /api/v1/auth/refresh`

Renueva el `accessToken` usando la cookie `refresh_token`. No necesita `Authorization` header.

> El browser envía la cookie automáticamente si la petición va al mismo dominio. En desarrollo desde frontend en `localhost:3000` hacia `localhost:8080`, puede requerir `credentials: 'include'` en fetch/axios.

**Body:** ninguno

**Respuesta `200`:**
```json
{
  "accessToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Errores:**
- `401` — cookie ausente, token inválido/expirado, o cuenta baneada

---

### `POST /api/v1/auth/logout`

Cierra sesión. Invalida el `refresh_token` e instruye al browser a borrar la cookie.

**Body:** ninguno

**Respuesta `200`:**
```json
{ "message": "Successfully logged out" }
```

---

### `POST /api/v1/auth/password/reset`

Solicita un email de recuperación de contraseña.

> Por seguridad, siempre devuelve `200` independientemente de si el email existe o no (para evitar enumeración de cuentas).

**Body:**
```json
{ "email": "string" }
```

**Respuesta `200`:**
```json
{ "message": "If an account with that email exists, a reset link has been sent." }
```

---

### `POST /api/v1/auth/password/reset/confirm`

Confirma el reset con el token recibido por email. El token expira en **15 minutos**.

**Body:**
```json
{
  "token": "string",
  "newPassword": "string"
}
```

**Respuesta `200`:**
```json
{ "message": "Password successfully reset." }
```

**Errores:**
- `400` — token inválido o expirado

---

## Usuarios — `/api/v1/users`

> Todos requieren 🔒 auth.

---

### `GET /api/v1/users/{userId}`

Obtiene el perfil de un usuario.

> El campo `email` solo se devuelve si el solicitante es el propio usuario o un ADMIN.

**Respuesta `200`:**
```json
{
  "id": "uuid",
  "username": "string",
  "email": "string | null",
  "role": "USER | ADMIN",
  "createdAt": "2026-04-30T10:00:00"
}
```

**Errores:**
- `404` — usuario no encontrado

---

### `PUT /api/v1/users/{userId}`

Actualiza el perfil. Solo el propio usuario puede editar su perfil.

**Body (todos los campos son opcionales):**
```json
{
  "username": "string",
  "email": "string"
}
```

**Validaciones:**
- `username`: 3–20 caracteres, solo letras, números y `_`
- `email`: formato email válido

**Respuesta `200`:** mismo objeto que `GET /users/{userId}` (con email incluido)

**Errores:**
- `401` — intentar editar el perfil de otro usuario
- `422` — username o email ya en uso

---

## Inventario — `/api/v1/users/{userId}/inventory`

> Todos requieren 🔒 auth.

El inventario contiene las cartas físicas de un usuario. Cada entrada (`user_card`) representa una carta con una condición y cantidad específica. Los trades intercambian estas entradas de inventario, no cartas del catálogo directamente.

---

### `GET /api/v1/users/{userId}/inventory`

Lista el inventario de un usuario. Cualquier usuario autenticado puede ver el inventario de cualquier otro.

**Query params:**

| Param | Tipo | Default | Descripción |
|-------|------|---------|-------------|
| `page` | int | `0` | Número de página |
| `size` | int | `20` | Tamaño (máx. 100) |

**Respuesta `200` (paginada):**
```json
{
  "content": [
    {
      "userCardId": "uuid",
      "cardId": "uuid | null",
      "customCardId": "uuid | null",
      "custom": false,
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
      "acquiredAt": "2026-04-30T10:00:00"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1
  }
}
```

> Si `custom: true`, los campos `cardId`, `setName`, `gameName` y `marketPrice` serán `null`. Los campos `customCardId`, `cardName`, `rarity` e `imageUrl` tendrán los datos de la carta personalizada.

**Errores:**
- `404` — usuario no encontrado

---

### `POST /api/v1/users/{userId}/inventory`

Añade una carta del catálogo al inventario. Solo el propio usuario puede añadir a su inventario.

**Content-Type:** `application/json`

**Body:**
```json
{
  "cardId": "uuid",
  "quantity": 1,
  "condition": "NEAR_MINT",
  "notes": "string | null"
}
```

**Validaciones:**
- `cardId`: obligatorio
- `quantity`: mínimo 1 (default: 1)
- `condition`: obligatorio, uno de los valores del enum `condition` (default: `NEAR_MINT`)

**Respuesta `201`:** mismo objeto que cada item del GET inventario

**Errores:**
- `401` — intentar añadir al inventario de otro usuario
- `404` — carta no encontrada en el catálogo

---

### `POST /api/v1/users/{userId}/inventory/custom`

Añade una carta personalizada al inventario (no pertenece al catálogo). Solo el propio usuario puede añadir a su inventario.

**Content-Type:** `multipart/form-data`

| Part | Content-Type | Descripción |
|------|-------------|-------------|
| `data` | `application/json` | Datos de la carta (ver body) |
| `image` | `image/jpeg`, `image/png`, `image/webp` | Foto de la carta (opcional, máx. 5 MB) |

**Body (part `data`):**
```json
{
  "name": "string",
  "cardNumber": "string | null",
  "rarity": "RARE",
  "condition": "NEAR_MINT",
  "quantity": 1,
  "attributes": "string JSON | null",
  "notes": "string | null"
}
```

**Validaciones:**
- `name`: obligatorio
- `rarity`: obligatorio, uno de los valores del enum `rarity`
- `condition`: obligatorio, uno de los valores del enum `condition`
- `quantity`: mínimo 1 (default: 1)
- Imagen: solo JPEG, PNG o WebP, máximo 5 MB

**Respuesta `201`:**
```json
{
  "userCardId": "uuid",
  "customCardId": "uuid",
  "custom": true,
  "cardName": "Mi carta personalizada",
  "cardNumber": "string | null",
  "rarity": "RARE",
  "imageUrl": "/uploads/cards/uuid.jpg | null",
  "imageSmallUrl": "null",
  "marketPrice": null,
  "setName": null,
  "gameName": null,
  "quantity": 1,
  "condition": "NEAR_MINT",
  "forTrade": false,
  "forSale": false,
  "notes": "string | null",
  "acquiredAt": "2026-04-30T10:00:00"
}
```

**Errores:**
- `401` — intentar añadir al inventario de otro usuario
- `422` — formato de imagen no soportado o tamaño excedido

---

### `DELETE /api/v1/users/{userId}/inventory/{userCardId}`

Elimina una carta del inventario. Solo el propio usuario puede eliminar sus cartas.

> No se puede eliminar una carta que esté participando en un trade activo (`PENDING` o `ACCEPTED`).

**Respuesta `204`** (sin body)

**Errores:**
- `404` — carta no encontrada en tu inventario
- `422` — la carta está en un trade activo

---

## Catálogo de cartas — `/api/v1/cards`

El catálogo está organizado en: **Juegos → Sets → Cartas**

Juegos pre-cargados: `Pokémon TCG`, `Magic: The Gathering`, `Yu-Gi-Oh!`

---

### `GET /api/v1/cards`

Lista el catálogo de cartas. **Público, no requiere auth.**

**Query params:**

| Param | Tipo | Descripción |
|-------|------|-------------|
| `search` | string | Búsqueda por nombre (parcial, insensible a mayúsculas) |
| `rarity` | string | Filtrar por rareza (exacto, ej: `RARE`) |
| `setId` | uuid | Filtrar por set |
| `page` | int | Default `0` |
| `size` | int | Default `20`, máx `100` |

**Respuesta `200` (paginada):**
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
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 102,
    "totalPages": 6
  }
}
```

---

### `GET /api/v1/cards/games`

Lista todos los juegos disponibles. **Público, no requiere auth.**

**Respuesta `200`:**
```json
[
  { "id": "uuid", "name": "Pokémon TCG" },
  { "id": "uuid", "name": "Magic: The Gathering" }
]
```

---

### `GET /api/v1/cards/sets`

Lista todos los sets. Se puede filtrar por juego. **Público, no requiere auth.**

**Query params:**

| Param | Tipo | Descripción |
|-------|------|-------------|
| `gameId` | uuid | Filtrar sets de un juego concreto (opcional) |

**Respuesta `200`:**
```json
[
  { "id": "uuid", "name": "Base Set", "gameName": "Pokémon TCG" }
]
```

---

### `GET /api/v1/cards/set/{setId}`

Lista las cartas de un set concreto paginadas. **Público, no requiere auth.**

**Query params:**

| Param | Tipo | Default | Descripción |
|-------|------|---------|-------------|
| `page` | int | `0` | Número de página |
| `size` | int | `20` | Tamaño (máx. 100) |
| `sortBy` | string | `cardNumber` | Campo de ordenación |

**Respuesta `200` (paginada):** lista de `CardDetailResponse` (mismo objeto que `GET /cards/{cardId}`)

**Errores:**
- `404` — set no encontrado

---

### `GET /api/v1/cards/{cardId}/owners`

Lista los usuarios que tienen una carta concreta en su inventario, excluyendo al propio usuario autenticado. Útil para encontrar con quién hacer trade. 🔒 **Requiere auth.**

**Respuesta `200`:**
```json
[
  {
    "userId": "uuid",
    "username": "string",
    "userCardId": "uuid",
    "condition": "NEAR_MINT",
    "quantity": 2
  }
]
```

**Errores:**
- `404` — carta no encontrada

---

### `GET /api/v1/cards/{cardId}`

Detalle de una carta del catálogo. **Público, no requiere auth.**

**Respuesta `200`:**
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
  "lastPriceUpdate": "2026-04-30T10:00:00 | null",
  "createdAt": "2026-04-30T10:00:00",
  "updatedAt": "2026-04-30T10:00:00"
}
```

**Errores:**
- `404` — carta no encontrada

---

### `POST /api/v1/cards` — 🔒 **Solo ADMIN**

Crea una carta en el catálogo. Soporta dos variantes según si se sube imagen o no.

**Variante sin imagen — `Content-Type: application/json`:**
```json
{
  "setId": "uuid",
  "name": "Pikachu",
  "cardNumber": "058/102",
  "rarity": "COMMON",
  "attributes": "{\"hp\": 40}",
  "marketPrice": 5.99
}
```

**Variante con imagen — `Content-Type: multipart/form-data`:**

| Part | Content-Type | Descripción |
|------|-------------|-------------|
| `data` | `application/json` | Los mismos campos del JSON anterior |
| `image` | `image/jpeg`, `image/png`, `image/webp` | Imagen (máx. 5 MB) |

**Validaciones:**
- `setId`: obligatorio
- `name`: obligatorio
- `rarity`: obligatorio, uno de los valores del enum

**Respuesta `201`:** `CardDetailResponse` (mismo que `GET /cards/{cardId}`)

**Errores:**
- `422` — ya existe una carta con ese nombre en ese set

---

### `PUT /api/v1/cards/{cardId}` — 🔒 **Solo ADMIN**

Actualiza una carta del catálogo. Mismo formato que `POST`. Todos los campos son opcionales.

**Respuesta `200`:** `CardDetailResponse`

**Errores:**
- `404` — carta no encontrada

---

### `DELETE /api/v1/cards/{cardId}` — 🔒 **Solo ADMIN**

Elimina una carta del catálogo. Elimina también todos los `user_cards` asociados.

**Respuesta `204`** (sin body)

**Errores:**
- `404` — carta no encontrada

---

## Trades — `/api/v1/trades`

> Todos requieren 🔒 auth.

### Concepto clave

Los trades intercambian entradas del inventario (`userCardId`), no cartas del catálogo. Un `userCardId` es una carta específica en el inventario de alguien, con su condición y cantidad.

**Flujo típico:**
1. Usuario A ve el inventario de Usuario B → `GET /users/{userId}/inventory`
2. Usuario A elige los `userCardId` que quiere de B y los que ofrece de los suyos
3. Usuario A propone el trade → `POST /trades`
4. Usuario B acepta/rechaza → `PUT /trades/{tradeId}/accept` o `/reject`
5. Si acepta, las cartas se transfieren automáticamente entre inventarios

**Reglas importantes:**
- No puedes tradear contigo mismo
- Máximo 20 cartas por trade (entre offered y requested)
- Máximo 50 propuestas de trade por día
- Un trade `PENDING` expira automáticamente a los **7 días**
- Al aceptar un trade, si no tienes suficientes cartas para cubrir otras propuestas pendientes, esas propuestas se cancelan automáticamente
- Puedes tener la misma carta en múltiples propuestas; el primero que acepte gana la carta
- No puedes eliminar una carta de tu inventario mientras esté en un trade activo

---

### `POST /api/v1/trades`

Propone un trade. El usuario autenticado es el proposer.

**Body:**
```json
{
  "receiverId": "uuid",
  "offeredCards": [
    { "userCardId": "uuid", "quantity": 1 }
  ],
  "requestedCards": [
    { "userCardId": "uuid", "quantity": 1 }
  ],
  "proposerNotes": "string | null"
}
```

**Validaciones:**
- `receiverId`: obligatorio, no puede ser el mismo que el proposer
- `offeredCards`: obligatorio, al menos 1 item
- `requestedCards`: obligatorio, al menos 1 item
- `quantity`: mínimo 1 (default: 1) por item
- Total de items (`offeredCards` + `requestedCards`): máximo 20
- Las cartas de `offeredCards` deben estar en tu inventario con cantidad suficiente
- Las cartas de `requestedCards` deben estar en el inventario del receiver con cantidad suficiente

**Respuesta `201`:**
```json
{
  "id": "uuid",
  "proposerId": "uuid",
  "proposerUsername": "string",
  "receiverId": "uuid",
  "receiverUsername": "string",
  "status": "PENDING",
  "proposerNotes": "string | null",
  "receiverNotes": null,
  "items": [
    {
      "userCardId": "uuid",
      "cardId": "uuid | null",
      "cardName": "Pikachu",
      "rarity": "COMMON",
      "imageUrl": "string | null",
      "fromUserId": "uuid",
      "fromUsername": "string",
      "quantity": 1
    }
  ],
  "proposedAt": "2026-04-30T10:00:00",
  "respondedAt": null,
  "completedAt": null,
  "createdAt": "2026-04-30T10:00:00",
  "updatedAt": "2026-04-30T10:00:00",
  "message": null
}
```

> En `items`, los que tienen `fromUserId === proposerId` son las cartas que ofrece el proposer. Los que tienen `fromUserId === receiverId` son las que solicita al receiver.

**Errores:**
- `422` — carta no en inventario, cantidad insuficiente, límite diario superado, trade con uno mismo, demasiados items

---

### `GET /api/v1/trades`

Lista los trades del usuario autenticado (como proposer o receiver).

**Query params:**

| Param | Tipo | Descripción |
|-------|------|-------------|
| `status` | string | Filtrar: `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED`, `COMPLETED` |
| `page` | int | Default `0` |
| `size` | int | Default `20`, máx `100` |

**Respuesta `200` (paginada):** lista de `TradeResponse`

---

### `GET /api/v1/trades/{tradeId}`

Detalle de un trade. Solo accesible por el proposer o el receiver.

**Respuesta `200`:** `TradeResponse`

**Errores:**
- `401` — no eres proposer ni receiver de este trade
- `404` — trade no encontrado

---

### `PUT /api/v1/trades/{tradeId}/accept`

El receiver acepta el trade. Las cartas se transfieren automáticamente.

> Solo el **receiver** puede aceptar. El trade debe estar en estado `PENDING` y no haber expirado (menos de 7 días).

**Body:** ninguno

**Respuesta `200`:** `TradeResponse` con `status: "ACCEPTED"` y `message: "Trade accepted. Inventory update is being processed."`

> El estado pasa a `COMPLETED` en segundos (proceso asíncrono). Si hubiera un fallo técnico, el sistema lo completa automáticamente en un máximo de 10 minutos.

**Errores:**
- `401` — no eres el receiver
- `422` — trade no está en PENDING, o ha expirado

---

### `PUT /api/v1/trades/{tradeId}/reject`

El receiver rechaza el trade.

> Solo el **receiver** puede rechazar. El trade debe estar en `PENDING`.

**Body (opcional):**
```json
{ "reason": "string" }
```

**Respuesta `200`:** `TradeResponse` con `status: "REJECTED"`. El `reason` se guarda en `receiverNotes`.

**Errores:**
- `401` — no eres el receiver
- `422` — trade no está en PENDING

---

### `DELETE /api/v1/trades/{tradeId}`

El proposer cancela el trade.

> Solo el **proposer** puede cancelar. El trade debe estar en `PENDING`.

**Body:** ninguno

**Respuesta `200`:** `TradeResponse` con `status: "CANCELLED"`

**Errores:**
- `401` — no eres el proposer
- `422` — trade no está en PENDING

---

## Admin — `/api/v1/admin` — 🔒 Solo `role: ADMIN`

> Todos los endpoints devuelven `403` si el usuario no tiene rol ADMIN.

---

### `GET /api/v1/admin/users`

Lista todos los usuarios de la plataforma.

**Query params:**

| Param | Tipo | Default |
|-------|------|---------|
| `page` | int | `0` |
| `size` | int | `20` (máx `100`) |

**Respuesta `200` (paginada):**
```json
{
  "content": [
    {
      "id": "uuid",
      "username": "string",
      "email": "string",
      "role": "USER | ADMIN",
      "isBanned": false,
      "createdAt": "2026-04-30T10:00:00",
      "deletedAt": null
    }
  ],
  "page": { "number": 0, "size": 20, "totalElements": 150, "totalPages": 8 }
}
```

---

### `GET /api/v1/admin/trades`

Lista todos los trades de la plataforma.

**Query params:**

| Param | Tipo | Descripción |
|-------|------|-------------|
| `status` | string | Filtrar por estado |
| `page` | int | Default `0` |
| `size` | int | Default `20`, máx `100` |

**Respuesta `200` (paginada):** lista de `TradeResponse`

---

### `PUT /api/v1/admin/users/{userId}/ban`

Banea o desbanea un usuario. Un usuario baneado no puede iniciar sesión ni renovar su token.

**Body:**
```json
{
  "banned": true,
  "reason": "string | null"
}
```

**Respuesta `200`:**
```json
{
  "id": "uuid",
  "username": "string",
  "isBanned": true,
  "updatedAt": "2026-04-30T10:00:00"
}
```

**Errores:**
- `404` — usuario no encontrado
- `422` — usuario ya está en ese estado (ej: intentar banear a alguien ya baneado)

---

### `GET /api/v1/admin/stats`

Estadísticas globales de la plataforma.

**Respuesta `200`:**
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
  "generatedAt": "2026-04-30T10:00:00"
}
```

---

## Imágenes (resumen rápido)

- **Proveedor:** Cloudinary
- **Carpeta cartas:** `cards/`
- **Carpeta usuarios:** `User_Img/`
- **Formatos aceptados:** JPEG, PNG, WebP
- **Tamaño máximo:** 5 MB
- **Las URLs devueltas** son públicas y permanentes — úsalas directamente en `<img src>`

---

## Paginación

Todos los listados devuelven:

```json
{
  "content": [...],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

## JWT — Decodificación

El `accessToken` es un JWT estándar. El payload contiene:

```json
{
  "sub": "uuid-del-usuario",
  "email": "usuario@example.com",
  "role": "USER | ADMIN",
  "type": "access",
  "iat": 1234567890,
  "exp": 1234571490
}
```

Decodifica el `role` para mostrar/ocultar secciones de admin en el frontend sin necesidad de hacer una petición adicional.

---

## Flujo recomendado para el frontend

### Autenticación

```
1. Login → guardar accessToken en memoria
2. Añadir interceptor HTTP:
   - En cada petición → añadir Authorization: Bearer <token>
   - Si respuesta es 401 → llamar a POST /auth/refresh → reintentar petición original
   - Si refresh también devuelve 401 → redirigir a login
3. Logout → llamar a POST /auth/logout → limpiar estado
```

### Trades

```
1. Ver inventario del otro usuario → GET /users/{userId}/inventory
2. El usuario selecciona qué userCardId ofrece y cuáles quiere
3. Proponer → POST /trades
4. Receiver ve su lista de trades → GET /trades?status=PENDING
5. Receiver acepta/rechaza → PUT /trades/{id}/accept o /reject
6. Tras aceptar, el estado pasa de ACCEPTED a COMPLETED en segundos
   → Puedes hacer polling en GET /trades/{id} hasta ver COMPLETED
```

### Inventario

```
1. Añadir carta del catálogo: buscar en GET /cards → POST /users/{id}/inventory
2. Añadir carta custom: POST /users/{id}/inventory/custom (multipart)
3. Ver propio inventario: GET /users/{id}/inventory
4. Ver inventario de otro usuario: GET /users/{otraId}/inventory
```
