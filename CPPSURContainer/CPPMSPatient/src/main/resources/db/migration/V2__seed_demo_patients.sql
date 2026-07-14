INSERT INTO patient (
    history_number, dni, first_names, last_names, birth_date,
    sex, phone, email, address, created_at, active
)
SELECT
    CONCAT('HC-', LPAD(next_history_number, 4, '0')),
    '76543210', 'Maria', 'Gonzalez Perez', '1990-05-15',
    'F', '999888777', 'maria.gonzalez@email.local', 'Lima',
    CURRENT_TIMESTAMP(6), TRUE
FROM (
    SELECT COALESCE(MAX(
        CASE WHEN history_number REGEXP '^HC-[0-9]+$'
            THEN CAST(SUBSTRING(history_number, 4) AS UNSIGNED)
        END
    ), 0) + 1 AS next_history_number
    FROM patient
) AS history_counter
WHERE NOT EXISTS (
    SELECT 1
    FROM patient
    WHERE dni = '76543210'
);

INSERT INTO patient (
    history_number, dni, first_names, last_names, birth_date,
    sex, phone, email, address, created_at, active
)
SELECT
    CONCAT('HC-', LPAD(next_history_number, 4, '0')),
    '12345678', 'Juan', 'Perez Torres', '1985-03-22',
    'M', '911222333', 'juan.perez@email.local', 'Lima',
    CURRENT_TIMESTAMP(6), TRUE
FROM (
    SELECT COALESCE(MAX(
        CASE WHEN history_number REGEXP '^HC-[0-9]+$'
            THEN CAST(SUBSTRING(history_number, 4) AS UNSIGNED)
        END
    ), 0) + 1 AS next_history_number
    FROM patient
) AS history_counter
WHERE NOT EXISTS (
    SELECT 1
    FROM patient
    WHERE dni = '12345678'
);

UPDATE history_number_sequence
SET next_value = GREATEST(
    next_value,
    COALESCE((
        SELECT MAX(CAST(SUBSTRING(history_number, 4) AS UNSIGNED)) + 1
        FROM patient
        WHERE history_number REGEXP '^HC-[0-9]+$'
    ), 1)
)
WHERE sequence_name = 'PATIENT_HISTORY';
