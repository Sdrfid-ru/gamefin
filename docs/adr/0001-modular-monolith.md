# ADR 0001 Modular monolith with offline first client

## Status

Accepted.

## Context

FinДруг needs authoritative financial rules, editable educational content and dependable child-safe behaviour, without the operational overhead of early microservices.

## Decision

Use a modular Ktor monolith over PostgreSQL. Organise backend code by domain and expose REST v1 contracts. The Android client maintains a Room projection and queued commands, while the backend owns ledger and final state. Each client financial command has an idempotency key.

## Consequences

The product can scale individual modules later without distributing transactions prematurely. It requires strict package boundaries, migration discipline and sync conflict tests from the start.
