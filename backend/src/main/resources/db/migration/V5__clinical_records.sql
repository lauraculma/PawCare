CREATE TABLE clinical_records (
    id               BIGSERIAL PRIMARY KEY,
    appointment_id   BIGINT       NOT NULL UNIQUE REFERENCES appointments (id) ON DELETE CASCADE,
    pet_id           BIGINT       NOT NULL REFERENCES pets (id) ON DELETE CASCADE,
    veterinarian_id  BIGINT       NOT NULL REFERENCES users (id),
    diagnosis        VARCHAR(240) NOT NULL,
    treatment        TEXT,
    prescription     TEXT,
    notes            TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_clinical_records_pet ON clinical_records (pet_id);
CREATE INDEX idx_clinical_records_vet ON clinical_records (veterinarian_id);
