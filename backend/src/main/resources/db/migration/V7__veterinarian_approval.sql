-- Existing accounts remain approved. New veterinarian registrations are held for admin review.
ALTER TABLE users
    ADD COLUMN approved BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_users_pending_veterinarian
    ON users (created_at)
    WHERE role = 'VETERINARIAN' AND approved = FALSE;
