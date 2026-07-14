ALTER TABLE queue_ticket
    ADD COLUMN appointment_id INT NULL,
    ADD COLUMN patient_id INT NULL;

CREATE UNIQUE INDEX uq_queue_ticket_appointment ON queue_ticket (appointment_id);
CREATE INDEX idx_queue_ticket_patient_created ON queue_ticket (patient_id, created_at);
