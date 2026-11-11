CREATE TABLE appointments (
    id               BIGSERIAL PRIMARY KEY,
    pet_id           BIGINT       NOT NULL REFERENCES pets (id) ON DELETE CASCADE,
    veterinarian_id  BIGINT       NOT NULL REFERENCES users (id),
    scheduled_at     TIMESTAMPTZ  NOT NULL,
    end_at           TIMESTAMPTZ  NOT NULL,
    duration_minutes INTEGER      NOT NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'SCHEDULED',
    reason           VARCHAR(240) NOT NULL,
    notes            TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_appointment_duration CHECK (duration_minutes BETWEEN 15 AND 180),
    CONSTRAINT chk_appointment_range CHECK (end_at > scheduled_at)
);

CREATE INDEX idx_appointments_pet ON appointments (pet_id);
CREATE INDEX idx_appointments_vet_date ON appointments (veterinarian_id, scheduled_at);
CREATE INDEX idx_appointments_status ON appointments (status);
