ALTER TABLE psychologist
    ADD COLUMN identity_subject VARCHAR(100);

UPDATE psychologist
SET identity_subject = 'psicologo'
WHERE license_number = 'CPP-12345';

-- Preserve manually-created rows from previous development environments while
-- assigning every psychologist a stable, non-null identity subject.
UPDATE psychologist
SET identity_subject = 'psychologist-' || id
WHERE identity_subject IS NULL;

ALTER TABLE psychologist
    ALTER COLUMN identity_subject SET NOT NULL;

CREATE UNIQUE INDEX uq_psychologist_identity_subject
    ON psychologist (LOWER(identity_subject));

INSERT INTO psychologist
    (first_names, last_names, license_number, phone, email, specialty_id, active, identity_subject)
SELECT 'Rosa', 'Quispe Flores', 'CPP-54321', '986543210',
       'rosa.quispe@clinica.local', id, TRUE, 'psicologo2'
FROM specialty
WHERE name = 'Psicologia Clinica';

INSERT INTO psychologist_schedule (psychologist_id, day_of_week, start_time, end_time, active)
SELECT p.id, day_number, TIME '08:00', TIME '19:00', TRUE
FROM psychologist p
CROSS JOIN generate_series(1, 6) AS day_number
WHERE p.identity_subject = 'psicologo2';
