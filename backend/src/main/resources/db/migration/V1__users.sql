CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(30)  NOT NULL,
    phone         VARCHAR(30),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_role ON users (role);
