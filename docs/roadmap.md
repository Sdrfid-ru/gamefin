# EPIC roadmap and dependencies

| Epic | Outcome | Depends on |
| --- | --- | --- |
| 01 Foundation | Gradle, conventions, CI, ADR, release config | - |
| 02 Design system | tokens and reusable accessible components | 01, brand inputs |
| 03-04 Backend and database | Ktor, migrations, health, auth/session | 01 |
| 05-08 Profile, pet, rendering, economy | first persistent game loop | 02-04, pet assets for final rendering |
| 09-10 Shop and goals | catalog, inventory and savings | 08, content model |
| 11-17 Task engine and games | tasks, daily/weekly, four games | 05, 08, content |
| 18-20 Progress, achievements, recommendations | skill profile and rule fallback | 11-17 |
| 21 ML contract | remote inference adapter with fallback | 20, ML ownership |
| 22-25 Analytics, CMS, sync, parents | operations and trust features | 03-04, relevant domains |
| 26-28 Security, accessibility, performance | release hardening | all active features |
| 29-30 Tests and release pipeline | E2E and AAB/APK delivery | all active features |

## M1 work breakdown

1. Foundation: version catalog, module boundaries, CI and quality gates.
2. Domain: pure Kotlin IDs, wallet ledger, pet state, goal deposit, task evaluation and deterministic recommendation with unit tests.
3. Local client: onboarding, home, decision, savings and task screens backed by Room/outbox contracts.
4. Server: migration, session/profile/pet/wallet/goal/task endpoints and idempotency store.
5. Integration: 100/30/80 E2E, restart and offline retry tests.

Every EPIC must pass build, tests, lint/static analysis, integration check and documentation update before the next one.

## Verification status

The repository verifies Android lint/debug assembly, domain unit tests and backend API tests with `./gradlew lint :core:domain:test :backend:test :app:assembleDebug`. CI provisions Android API 37.2. PostgreSQL integration tests and signed AAB delivery remain dependent on a database service and release-owner secrets.
