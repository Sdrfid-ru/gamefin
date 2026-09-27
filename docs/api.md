# REST API v1

Base path: `/api/v1`. JSON uses `snake_case`. Every error has `code`, `message_key`, `correlation_id` and optional field errors. Mutating requests require `Idempotency-Key`; response includes `X-Correlation-Id` and a resource version.

| Route | Purpose |
| --- | --- |
| `POST /sessions/anonymous` | create anonymous child session |
| `GET/PATCH /profile` | read/update minimal profile |
| `GET/POST/PATCH /pets` | pet creation and customization |
| `GET /pet-state` | pet state projection |
| `POST /pet-actions` | feed, play, care, sleep command |
| `GET /wallet` | balance and ledger cursor |
| `GET /transactions` | paginated immutable history |
| `GET /shop-items`, `POST /purchases` | catalog and purchase command |
| `GET/POST/PATCH /goals` | goals and savings deposit command |
| `GET /tasks/next`, `POST /task-attempts` | recommendation and evaluated answer |
| `GET /challenges/daily`, `GET /challenges/weekly` | scheduled content |
| `GET /progress`, `GET /achievements` | child progress projections |
| `GET /parent-summary` | consent-gated, pressure-free summary |
| `POST /sync/commands` | ordered offline command batch |
| `POST /analytics/events` | batched pseudonymous events |
| `GET/POST/PATCH /admin/content/*` | role-gated versioned CMS resources |
| `GET /health/live`, `GET /health/ready` | deployment health |

`POST /purchases`, `POST /goals/{id}/deposits`, `POST /task-attempts` and `/sync/commands` are transactional commands. They return the authoritative wallet and affected projections. Database entities never appear on the wire; dedicated DTOs are mapped at the API boundary.

`POST /sessions/anonymous` creates an opaque anonymous identity and session token. It intentionally accepts no child PII; production persistence stores a token digest rather than the token itself, with explicit expiry and revocation.

`GET /session` accepts `Authorization: Bearer <session_token>` and returns only the opaque anonymous user ID.

## Idempotency

Every mutating endpoint requires an `Idempotency-Key` header containing a UUID. The first response for a key is the retry result; malformed keys are rejected at the HTTP boundary.

All game commands and task recommendations require the same Bearer session token. Their wallet, goal, skill and idempotency state are scoped to that anonymous actor.
