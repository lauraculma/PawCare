ALTER TABLE users ADD COLUMN cedula VARCHAR(20);
CREATE UNIQUE INDEX idx_users_cedula ON users (cedula) WHERE cedula IS NOT NULL;

CREATE SEQUENCE pet_public_id_seq START WITH 0 MINVALUE 0;
ALTER TABLE pets ADD COLUMN public_id VARCHAR(5);
WITH numbered AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) - 1 AS public_number
    FROM pets
)
UPDATE pets p
SET public_id = LPAD(numbered.public_number::TEXT, 5, '0')
FROM numbered
WHERE p.id = numbered.id;
SELECT setval('pet_public_id_seq', GREATEST((SELECT COUNT(*) FROM pets), 1) - 1,
              (SELECT COUNT(*) FROM pets) > 0);
ALTER TABLE pets ALTER COLUMN public_id SET DEFAULT LPAD(nextval('pet_public_id_seq')::TEXT, 5, '0');
ALTER TABLE pets ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE pets ADD CONSTRAINT uq_pets_public_id UNIQUE (public_id);
