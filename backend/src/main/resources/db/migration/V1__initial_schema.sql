CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE wallets (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE RESTRICT,
    available_coins INTEGER NOT NULL DEFAULT 0 CHECK (available_coins >= 0),
    reserved_coins INTEGER NOT NULL DEFAULT 0 CHECK (reserved_coins >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    idempotency_key UUID NOT NULL,
    transaction_type TEXT NOT NULL,
    spending_category TEXT NOT NULL,
    delta_available INTEGER NOT NULL,
    delta_reserved INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, idempotency_key)
);
CREATE INDEX transactions_user_created_idx ON transactions (user_id, created_at DESC);

CREATE TABLE processed_commands (
    actor_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    idempotency_key UUID NOT NULL,
    response_body JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (actor_id, idempotency_key)
);
