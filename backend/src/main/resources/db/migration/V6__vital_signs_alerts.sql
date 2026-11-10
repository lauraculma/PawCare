CREATE TABLE vital_signs (
    id             BIGSERIAL PRIMARY KEY,
    pet_id         BIGINT           NOT NULL REFERENCES pets (id) ON DELETE CASCADE,
    collar_id      VARCHAR(60),
    heart_rate     INTEGER,
    spo2           INTEGER,
    temperature_c  DOUBLE PRECISION,
    activity_level VARCHAR(30),
    recorded_at    TIMESTAMPTZ      NOT NULL,
    created_at     TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vital_signs_pet_date ON vital_signs (pet_id, recorded_at DESC);

CREATE TABLE alerts (
    id               BIGSERIAL PRIMARY KEY,
    pet_id           BIGINT       NOT NULL REFERENCES pets (id) ON DELETE CASCADE,
    vital_sign_id    BIGINT       REFERENCES vital_signs (id) ON DELETE CASCADE,
    type             VARCHAR(40)  NOT NULL,
    severity         VARCHAR(20)  NOT NULL,
    message          VARCHAR(240) NOT NULL,
    acknowledged     BOOLEAN      NOT NULL DEFAULT FALSE,
    acknowledged_by  BIGINT       REFERENCES users (id),
    acknowledged_at  TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alerts_pet_date ON alerts (pet_id, created_at DESC);
CREATE INDEX idx_alerts_ack ON alerts (acknowledged);
