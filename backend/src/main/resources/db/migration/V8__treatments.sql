CREATE TABLE treatments (
    id                  BIGSERIAL PRIMARY KEY,
    clinical_record_id  BIGINT       NOT NULL REFERENCES clinical_records (id) ON DELETE CASCADE,
    medication          VARCHAR(120) NOT NULL,
    dosage              VARCHAR(120) NOT NULL,
    frequency           VARCHAR(120) NOT NULL,
    duration            VARCHAR(120),
    instructions        TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_treatments_clinical_record ON treatments (clinical_record_id);
