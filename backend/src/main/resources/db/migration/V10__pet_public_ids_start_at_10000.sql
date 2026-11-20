ALTER SEQUENCE pet_public_id_seq MAXVALUE 99999 NO CYCLE;

SELECT setval(
    'pet_public_id_seq',
    GREATEST(
        9999,
        COALESCE((SELECT MAX(public_id::INTEGER) FROM pets), 9999),
        (SELECT CASE WHEN is_called THEN last_value ELSE last_value - 1 END FROM pet_public_id_seq)
    ),
    TRUE
);
