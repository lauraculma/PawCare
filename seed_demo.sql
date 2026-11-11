-- Seed demo PawCare
-- Users
INSERT INTO users (full_name, email, password_hash, role, phone, active)
VALUES 
  ('Ana García','ana@example.com','$2a$10$8K1p/a0dhrxiOwP9GWKPOeXG5JXK9JkK9JkK9JkK9JkK9JkK9JkK9','OWNER','3001234567',true),
  ('Dr. Laura Méndez','laura.vet@example.com','$2a$10$8K1p/a0dhrxiOwP9GWKPOeXG5JXK9JkK9JkK9JkK9JkK9JkK9JkK9','VETERINARIAN','3007654321',true)
ON CONFLICT (email) DO NOTHING;

-- Pets for owner id 2 and 4
INSERT INTO pets (owner_id, name, species, breed, birth_date, gender, microchip, status, active)
VALUES
  (2,'Luna','Gato','Siamés','2019-03-15','F',NULL,'ACTIVE',true),
  (2,'Rocky','Perro','Bulldog','2018-07-22','M','987654321012345','ACTIVE',true)
ON CONFLICT DO NOTHING;

-- Appointments demo
INSERT INTO appointments (pet_id, veterinarian_id, scheduled_at, end_at, duration_minutes, status, reason, created_at, updated_at)
SELECT p.id, u.id, NOW() + INTERVAL '2 days', NOW() + INTERVAL '2 days 30 minutes', 30, 'SCHEDULED','Control de rutina', NOW(), NOW()
FROM pets p, users u
WHERE p.name='Luna' AND u.email='laura.vet@example.com'
ON CONFLICT DO NOTHING;

-- Vital signs demo
INSERT INTO vital_signs (pet_id, collar_id, heart_rate, spo2, temperature_c, activity_level, recorded_at)
SELECT p.id, 'COL-001', 78, 98, 38.6, 'MODERATE', NOW() - INTERVAL '1 hour'
FROM pets p
WHERE p.name='Luna'
ON CONFLICT DO NOTHING;
