# PostgreSQL schema

All primary keys are UUID. Timestamps are `timestamptz` in UTC. Monetary values are integer virtual coins, never floating point.

| Table | Purpose | Important constraints and indexes |
| --- | --- | --- |
| `users` | anonymous identity and lifecycle | unique public ID, `created_at` |
| `user_sessions` | anonymous access sessions | token digest is unique; expiration and revocation are explicit |
| `profiles` | age band, locale, settings pointer | one-to-one user, no PII |
| `pets` | species, name, appearance, level | one active pet per user |
| `pet_states` | 0-100 state projection | one row per pet, check for each value |
| `wallets` | spendable/reserved balance projection | one wallet per user, non-negative checks |
| `transactions` | immutable ledger | unique `(user_id,idempotency_key)`, indexed user/time |
| `inventory` | owned catalog items | unique `(user_id,shop_item_id)` |
| `shop_items` | versioned purchasable content | publish state, price non-negative |
| `savings_goals` | versioned target catalog | goal cost positive |
| `user_goals` | active/completed savings | one active goal per user, balance bounded by target |
| `tasks` | versioned scenario root | task key + content version unique |
| `task_options` | choices and consequences | ordered per task version |
| `task_attempts` | durable evaluation result | user/task/time index, idempotency key |
| `competencies` | external framework mapping | stable competency code unique |
| `skill_profiles` | per-user skill estimates | unique `(user_id,skill_code)` |
| `achievements` | declarative definitions | code + version unique |
| `user_achievements` | unlock history | unique `(user_id,achievement_id)` |
| `recommendations` | chosen next task and reason | one current recommendation per user |
| `analytics_events` | pseudonymous product events | user/time/type index, retention policy |
| `processed_commands` | idempotency command result | unique actor/key, response digest |

Foreign keys use `RESTRICT` for financial/content history and controlled `CASCADE` only for disposable local projections. Flyway owns all schema changes. Content is versioned with `draft`, `published`, `archived`; attempts retain the exact content version evaluated.
