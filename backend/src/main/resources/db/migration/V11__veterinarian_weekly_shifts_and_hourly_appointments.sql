UPDATE appointments
SET duration_minutes = 60,
    end_at = scheduled_at + INTERVAL '60 minutes';

ALTER TABLE appointments
    ADD CONSTRAINT chk_appointment_one_hour CHECK (duration_minutes = 60);

CREATE TABLE veterinarian_shifts (
    id BIGSERIAL PRIMARY KEY,
    veterinarian_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL CHECK (start_time >= TIME '07:00' AND start_time <= TIME '17:00'
                                    AND EXTRACT(MINUTE FROM start_time) = 0
                                    AND EXTRACT(SECOND FROM start_time) = 0),
    CONSTRAINT uq_vet_shift_weekday UNIQUE (veterinarian_id, day_of_week)
);
