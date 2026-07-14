ALTER TABLE therapeutic_process ADD COLUMN initial_appointment_id INT NULL;

ALTER TABLE clinical_discharge_report
    ADD COLUMN ticket_number VARCHAR(10) NULL,
    ADD COLUMN ticket_issued_at TIMESTAMP NULL;

CREATE INDEX idx_discharge_productivity_period
    ON clinical_discharge_report (discharged_at, ticket_issued_at);
