# Feature Specification: Card Trading Platform

**Feature Branch**: `001-card-trading-platform`
**Created**: 2026-04-23
**Status**: Draft
**Input**: User description: "A platform where users can trade collectible cards with each other. Users build their inventory, browse available cards, create trade offers, and complete trades asynchronously via Kafka events."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - User Registration and Login (Priority: P1)

A new user visits the platform and creates an account with a username, email, and password. After registering, they will receive a mail to confirming the account, they log in and receive a session that persists across visits. They can also reset their password if forgotten.

**Why this priority**: Authentication is the foundation of all other features. Without it, no user can access inventory, browse trades, or interact with other users. Every other story depends on this one.

**Independent Test**: Can be fully tested by registering a new user, logging in, receiving tokens, and verifying that protected endpoints are accessible — delivers a working authentication system independently of any card or trade functionality.

**Acceptance Scenarios**:

1. **Given** a visitor provides a unique username, valid email, and a password meeting complexity rules, **When** they submit the registration form, **Then** they receive a mail to confirming the registration, a new account is created and the system confirms success.
2. **Given** a registered user provides correct credentials, **When** they log in, **Then** the system returns an access token and a refresh token valid for their respective durations.
3. **Given** a user has a valid refresh token, **When** they request a new access token, **Then** a new access token is issued without requiring re-login.
4. **Given** a user requests a password reset with their registered email, **When** the request is submitted, **Then** the system initiates the reset process and the user can set a new password.
5. **Given** a user attempts to log in 5 times with incorrect credentials within 15 minutes, **When** they try again, **Then** the system blocks further attempts until the window expires.
6. **Given** a user provides a password shorter than 8 characters or missing uppercase, lowercase, or a number, **When** they register, **Then** registration is rejected with a descriptive error.

---

### User Story 2 - Browse and Search the Card Catalog (Priority: P2)

A logged-in user wants to explore what cards exist in the system. They can page through all cards, search by name, and filter by rarity, card type, or edition. Clicking a card shows its full details including description, rarity, and image.

**Why this priority**: Users need to know what cards exist before they can manage their inventory or create trades. The catalog is a core discovery surface and a prerequisite for informed trading.

**Independent Test**: Can be fully tested by browsing the card list, applying filters, searching by keyword, and viewing a card detail page — delivers a usable read-only catalog independently of any trade functionality.

**Acceptance Scenarios**:

1. **Given** a logged-in user opens the card catalog, **When** the page loads, **Then** a paginated list of all cards is displayed with name, rarity, and image.
2. **Given** a user types a search term, **When** they submit the search, **Then** only cards whose name contains the term are returned.
3. **Given** a user applies a rarity filter (e.g., LEGENDARY), **When** the filter is applied, **Then** only cards of that rarity are shown.
4. **Given** a user clicks on a specific card, **When** the detail page loads, **Then** the card's name, description, rarity, edition, type, and image are displayed.
5. **Given** the catalog has been recently loaded, **When** the same query is repeated within the cache window, **Then** results are returned from cache with no additional database load.

---

### User Story 3 - Manage Personal Inventory (Priority: P3)

A logged-in user can view all the cards they own, how many copies of each, when they acquired them, and how (system grant, trade, purchase). They can also view another user's inventory to identify cards for a potential trade.

**Why this priority**: Inventory is the input to every trade. Users must be able to see what they own before creating or responding to trade offers.

**Independent Test**: Can be fully tested by viewing the authenticated user's inventory, checking quantities and acquisition history, and viewing another user's public inventory — delivers a complete inventory viewer independently of trading actions.

**Acceptance Scenarios**:

1. **Given** a user has cards in their inventory, **When** they view their collection, **Then** each card is listed with its name, quantity owned, acquisition date, and acquisition source.
2. **Given** a user owns 3 copies of a card, **When** they view their inventory, **Then** the quantity shown is 3.
3. **Given** a user views another user's profile, **When** they navigate to that user's inventory, **Then** the cards owned by that user are visible.
4. **Given** a card is added to a user's inventory by the system, **When** the user views their collection, **Then** the acquisition source shows "SYSTEM" and the date is recorded.

---

### User Story 4 - Create a Trade Offer (Priority: P4)

User A browses User B's inventory and creates a trade offer by selecting cards they will give (from their own inventory) and cards they want in return (from User B's inventory). The system validates ownership before the offer is submitted.

**Why this priority**: Trade creation is the primary value proposition of the platform. It depends on authentication (P1) and inventory visibility (P3) being functional first.

**Independent Test**: Can be fully tested by having two users with populated inventories, creating a trade offer, and verifying the offer appears as PENDING for both users — delivers end-to-end trade initiation independently of the accept/reject flows.

**Acceptance Scenarios**:

1. **Given** User A owns the cards they are offering and User B owns the cards being requested, **When** User A submits a trade offer, **Then** a trade is created with status PENDING and both users can see it.
2. **Given** User A attempts to offer a card they do not own (or insufficient quantity), **When** the offer is submitted, **Then** the system rejects it with a clear error.
3. **Given** User B does not own the requested cards in sufficient quantity, **When** User A submits the offer, **Then** the system rejects it with a clear error.
4. **Given** a user tries to trade with themselves, **When** the offer is submitted, **Then** the system rejects it.
5. **Given** a trade offer contains more than 20 card items, **When** the offer is submitted, **Then** the system rejects it.
6. **Given** a user has already created 50 trades today, **When** they attempt another, **Then** the system blocks the creation.

---

### User Story 5 - Accept, Reject, or Cancel a Trade (Priority: P5)

User B receives a trade offer notification. They can view the offer details, accept or reject it. If accepted, the system processes the card exchange asynchronously. User A can cancel their own pending offer at any time before it is accepted.

**Why this priority**: Trade resolution completes the core trading loop. It depends on trade creation (P4) being functional.

**Independent Test**: Can be fully tested with a PENDING trade: accepting triggers inventory swaps and COMPLETED status; rejecting marks it REJECTED; cancelling by the offerer marks it CANCELLED — all verifiable independently.

**Acceptance Scenarios**:

1. **Given** a PENDING trade exists, **When** User B accepts it, **Then** the trade status transitions to ACCEPTED and then to COMPLETED after inventory updates succeed.
2. **Given** a trade is ACCEPTED, **When** the inventory exchange is processed, **Then** offered cards are removed from User A and added to User B; requested cards are removed from User B and added to User A.
3. **Given** inventory update fails during trade completion, **When** the failure occurs, **Then** the trade is marked FAILED and no partial inventory changes persist.
4. **Given** a PENDING trade exists, **When** User B rejects it, **Then** the trade status becomes REJECTED and no cards are moved.
5. **Given** a PENDING trade exists, **When** User A cancels it, **Then** the trade status becomes CANCELLED.
6. **Given** a trade is no longer PENDING (e.g., ACCEPTED or COMPLETED), **When** User A tries to cancel it, **Then** the system rejects the cancellation.
7. **Given** a trade has been PENDING for 7 days with no response, **When** the expiry threshold is reached, **Then** the trade is automatically cancelled.

---

### User Story 6 - Receive Trade Notifications (Priority: P6)

Users receive notifications when a trade offer is created for them, when their offer is accepted or rejected, and when a trade is completed. Email is the primary delivery channel for V1.

**Why this priority**: Notifications close the asynchronous loop, ensuring users act on trade events without polling the platform. Depends on trade flows (P4, P5) being operational.

**Independent Test**: Can be fully tested by triggering each trade event and verifying that the correct notification is delivered to the right user via email — independent of UI or catalog features.

**Acceptance Scenarios**:

1. **Given** a trade offer is created targeting User B, **When** the event is processed, **Then** User B receives an email notification about the incoming offer.
2. **Given** User B accepts a trade, **When** the event is processed, **Then** User A receives a notification that their offer was accepted.
3. **Given** User B rejects a trade, **When** the event is processed, **Then** User A receives a notification that their offer was rejected.
4. **Given** a trade reaches COMPLETED status, **When** the event is processed, **Then** both User A and User B receive a completion notification.
5. **Given** an event fails to deliver, **When** the maximum retry attempts are exhausted, **Then** the event is routed to a dead letter queue for manual review.

---

### User Story 7 - Admin Card and User Management (Priority: P7)

An admin user can add new cards to the catalog, update or remove existing ones, and view all users and their trade activity. Admins can also ban or unban users and view system-wide statistics.

**Why this priority**: Admin tools enable platform operations and content management. They are important but do not block any user-facing trading flows.

**Independent Test**: Can be fully tested by an admin account creating a card (visible in catalog), banning a user (login blocked), and viewing trade stats — independent of regular user flows.

**Acceptance Scenarios**:

1. **Given** an admin submits a new card with valid attributes, **When** the creation is processed, **Then** the card appears in the public catalog.
2. **Given** an admin updates an existing card's description or rarity, **When** the update is saved, **Then** the card detail page reflects the change.
3. **Given** an admin deletes a card, **When** the deletion is processed, **Then** the card no longer appears in the catalog.
4. **Given** an admin bans a user, **When** the ban is applied, **Then** that user cannot log in or perform actions on the platform.
5. **Given** an admin views system statistics, **When** the stats page loads, **Then** total users, total trades, and trade status breakdowns are displayed.
6. **Given** a non-admin user attempts to access an admin endpoint, **When** the request is made, **Then** the system rejects it with an authorization error.

---

### Edge Cases

- What happens when a user's inventory changes between trade offer creation and acceptance (e.g., cards consumed by another accepted trade)?
- How does the system handle duplicate trade offers between the same two users for the same cards?
- What happens when a Kafka event is delivered more than once (idempotency)?
- How does the system behave when the card catalog cache is stale but the database is updated?
- What happens when a user account is banned while they have active pending trades?
- How are quantities handled when a user owns exactly the right amount of cards being offered (no surplus)?

## Requirements *(mandatory)*

### Functional Requirements

**Authentication & Users**

- **FR-001**: System MUST allow users to register with a unique username (3–20 characters), unique email address, and a password meeting complexity rules (minimum 8 characters, at least one uppercase letter, one lowercase letter, and one number).
- **FR-002**: System MUST authenticate registered users via email/password and return a short-lived access token and a longer-lived refresh token upon successful login.
- **FR-003**: System MUST allow users to exchange a valid refresh token for a new access token without re-entering credentials.
- **FR-004**: System MUST invalidate tokens upon logout.
- **FR-005**: System MUST support a password reset flow initiated by email.
- **FR-005a**: System MUST send a registration confirmation email to the user upon successful account creation.
- **FR-006**: System MUST enforce rate limiting on login attempts: no more than 5 failed attempts per user per 15-minute window.
- **FR-007**: System MUST support two roles: USER and ADMIN, with role-based access enforcement on all protected endpoints.
- **FR-008**: Users MUST be able to view and update their own profile (username, email).

**Card Catalog**

- **FR-009**: System MUST display all cards in a paginated list with filtering by rarity, card type, and edition, and full-text search by card name.
- **FR-010**: System MUST display a card detail page showing name, description, rarity, edition, card type, and image.
- **FR-011**: System MUST cache card catalog results to reduce repeated database load, with a time-based expiry.
- **FR-012**: ADMIN users MUST be able to create, update, and delete cards in the catalog.

**Inventory**

- **FR-013**: System MUST display a user's card inventory including card name, quantity owned, acquisition date, and acquisition source (SYSTEM, TRADE, or PURCHASE).
- **FR-014**: System MUST allow authenticated users to view another user's public inventory.
- **FR-015**: System MUST prevent a user's card quantity from falling below zero.
- **FR-016**: System MUST record acquisition history (source and timestamp) whenever a card is added to a user's inventory.

**Trading**

- **FR-017**: System MUST allow a user to create a trade offer specifying cards they give (OFFER side) and cards they want (REQUEST side) from a specific other user.
- **FR-018**: System MUST validate at offer creation that: (a) the offerer owns sufficient quantity of all offered cards, (b) the receiver owns sufficient quantity of all requested cards, (c) the offerer and receiver are different users, (d) the trade does not exceed 20 card items, and (e) the offerer has not exceeded 50 trades in the current calendar day.
- **FR-019**: System MUST allow the receiver to accept a PENDING trade offer.
- **FR-020**: System MUST allow the receiver to reject a PENDING trade offer.
- **FR-021**: System MUST allow the offerer to cancel their own PENDING trade offer.
- **FR-022**: System MUST process inventory exchanges atomically upon trade acceptance: remove offered cards from offerer and add to receiver; remove requested cards from receiver and add to offerer.
- **FR-023**: System MUST mark a trade as FAILED and leave inventories unchanged if the inventory exchange cannot be completed successfully.
- **FR-024**: System MUST automatically expire PENDING trades that have not been accepted within 7 days.
- **FR-025**: Users MUST be able to view their trade history including status (PENDING, ACCEPTED, REJECTED, COMPLETED, CANCELLED, FAILED) and the cards involved.

**Notifications**

- **FR-026**: System MUST send an email notification to the receiver when a new trade offer is created.
- **FR-027**: System MUST send email notifications to the relevant user(s) when a trade is accepted, rejected, or completed.
- **FR-028**: System MUST retry failed notification deliveries up to 3 times before routing to a dead letter queue.

**Admin**

- **FR-029**: ADMIN users MUST be able to view a list of all users and all trades platform-wide.
- **FR-030**: ADMIN users MUST be able to ban or unban any user account; banned users cannot log in or perform any platform action.
- **FR-031**: ADMIN users MUST be able to view aggregate platform statistics (user count, trade counts by status).

### Key Entities

- **User**: Represents a registered platform member. Has a role (USER or ADMIN), credentials, and a soft-delete field. Can own cards and participate in trades.
- **Card**: Represents a collectible card in the system catalog. Defined by name, description, rarity, edition, card type, and an image. Managed exclusively by admins.
- **UserCard (Inventory Entry)**: Represents ownership of a specific card by a specific user. Tracks quantity and acquisition metadata. A user-card pair is unique (one row per user per card, quantity increments).
- **Trade**: Represents a bilateral exchange proposal between two users. Has a lifecycle: PENDING → ACCEPTED → COMPLETED (or REJECTED / CANCELLED / FAILED).
- **TradeItem**: Represents a single card-quantity line within a trade, tagged as either the OFFER side (what the offerer gives) or the REQUEST side (what the offerer wants).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new user can complete registration and log in within 2 minutes from first visiting the platform.
- **SC-002**: A user can find a specific card by name or filter within 3 interactions (search or filter application).
- **SC-003**: A trade offer can be created, accepted, and reflected in both users' inventories within 30 seconds end-to-end under normal load.
- **SC-004**: The platform supports at least 1,000 simultaneous active users without noticeable degradation in response times.
- **SC-005**: 95% of all user-facing actions complete and return a result in under 1 second as experienced by the user.
- **SC-006**: Inventory balances are always consistent — no trade results in cards being duplicated or permanently lost, even under failure conditions.
- **SC-007**: Notification emails are delivered to the correct recipient within 60 seconds of the triggering trade event under normal operating conditions.
- **SC-008**: Failed trade events (after retries) are recoverable — no event is silently dropped; all failures are routable to a dead letter queue for manual resolution.
- **SC-009**: Admin catalog changes (add, update, delete) are reflected in the public card catalog within 5 minutes.
- **SC-010**: 100% of endpoints that require authentication reject unauthenticated or unauthorized requests without exposing sensitive data.

## Assumptions

- Users have access to a valid email address for registration and notification delivery.
- Cards are seeded into the catalog by admins or through a data migration; end users cannot create cards.
- A user's initial inventory (first cards) is granted by an admin or system process; self-purchase is out of scope for V1.
- Email is the only notification channel for V1; push notifications and in-app real-time alerts are deferred.
- Real-time chat, a card marketplace with pricing, user reputation/rating, wishlists, and trade templates are explicitly out of scope for V1.
- Mobile app support is out of scope for V1; the platform targets web browsers.
- The card image files are hosted externally and referenced by URL; the platform does not manage image storage directly.
- Trade expiry (7-day auto-cancel) is enforced by a background scheduled process, not in real time.
- All monetary transactions (purchase flows) are out of scope for V1; acquisition source PURCHASE is reserved for future use.
- The platform assumes a single deployment region for V1; multi-region failover is not in scope.
