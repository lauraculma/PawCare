CREATE TABLE pets (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(80)  NOT NULL,
    species     VARCHAR(40)  NOT NULL,
    breed       VARCHAR(80),
    birth_date  DATE,
    weight_kg   NUMERIC(5, 2),
    sex         VARCHAR(20),
    collar_id   VARCHAR(60) UNIQUE,
    notes       TEXT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pets_owner ON pets (owner_id);
CREATE INDEX idx_pets_name ON pets (LOWER(name));
