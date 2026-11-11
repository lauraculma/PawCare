-- Datos de demostracion para el esquema actual de PawCare.
-- Los usuarios deben existir previamente (por ejemplo, ejecutando seed_demo.sh).
-- Uso: psql ... -v owner_email='test@example.com' -v vet_email='vet@test.com' -f seed_demo.sql

INSERT INTO pets (owner_id, name, species, breed, birth_date, sex, collar_id, notes, active)
SELECT u.id, 'Luna', 'Gato', 'Siames', DATE '2019-03-15', 'Hembra', 'COL-001',
       'Mascota de demostracion', TRUE
FROM users u
WHERE lower(u.email) = lower(:'owner_email')
  AND NOT EXISTS (SELECT 1 FROM pets p WHERE p.collar_id = 'COL-001');

INSERT INTO pets (owner_id, name, species, breed, birth_date, sex, collar_id, notes, active)
SELECT u.id, 'Rocky', 'Perro', 'Bulldog', DATE '2018-07-22', 'Macho', 'COL-002',
       'Mascota de demostracion', TRUE
FROM users u
WHERE lower(u.email) = lower(:'owner_email')
  AND NOT EXISTS (SELECT 1 FROM pets p WHERE p.collar_id = 'COL-002');

INSERT INTO appointments (pet_id, veterinarian_id, scheduled_at, end_at, duration_minutes,
                          status, reason, notes)
SELECT p.id, v.id, NOW() + INTERVAL '2 days', NOW() + INTERVAL '2 days 30 minutes',
       30, 'SCHEDULED', 'Control de rutina', 'Cita de demostracion'
FROM pets p
JOIN users o ON o.id = p.owner_id
JOIN users v ON lower(v.email) = lower(:'vet_email')
WHERE p.collar_id = 'COL-001'
  AND lower(o.email) = lower(:'owner_email')
  AND NOT EXISTS (
      SELECT 1 FROM appointments a
      WHERE a.pet_id = p.id
        AND a.veterinarian_id = v.id
        AND a.reason = 'Control de rutina'
  );

INSERT INTO vital_signs (pet_id, collar_id, heart_rate, spo2, temperature_c,
                         activity_level, recorded_at)
SELECT p.id, p.collar_id, 78, 98, 38.6, 'MODERATE', NOW() - INTERVAL '1 hour'
FROM pets p
JOIN users o ON o.id = p.owner_id
WHERE p.collar_id = 'COL-001'
  AND lower(o.email) = lower(:'owner_email')
  AND NOT EXISTS (
      SELECT 1 FROM vital_signs v
      WHERE v.pet_id = p.id AND v.recorded_at > NOW() - INTERVAL '2 hours'
  );
