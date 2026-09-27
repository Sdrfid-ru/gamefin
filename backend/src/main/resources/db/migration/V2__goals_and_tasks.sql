CREATE TABLE savings_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_key TEXT NOT NULL,
    content_version INTEGER NOT NULL,
    target_coins INTEGER NOT NULL CHECK (target_coins > 0),
    publish_state TEXT NOT NULL CHECK (publish_state IN ('draft', 'published', 'archived')),
    UNIQUE (content_key, content_version)
);

CREATE TABLE user_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    savings_goal_id UUID NOT NULL REFERENCES savings_goals(id) ON DELETE RESTRICT,
    saved_coins INTEGER NOT NULL DEFAULT 0 CHECK (saved_coins >= 0),
    status TEXT NOT NULL CHECK (status IN ('active', 'completed', 'cancelled')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX user_goals_one_active_goal_idx ON user_goals (user_id) WHERE status = 'active';

CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_key TEXT NOT NULL,
    content_version INTEGER NOT NULL,
    competency_code TEXT NOT NULL,
    difficulty SMALLINT NOT NULL CHECK (difficulty BETWEEN 1 AND 5),
    publish_state TEXT NOT NULL CHECK (publish_state IN ('draft', 'published', 'archived')),
    content JSONB NOT NULL,
    UNIQUE (content_key, content_version)
);

CREATE TABLE task_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE RESTRICT,
    idempotency_key UUID NOT NULL,
    evaluation JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, idempotency_key)
);
