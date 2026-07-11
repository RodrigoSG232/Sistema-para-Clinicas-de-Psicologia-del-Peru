CREATE TABLE history_number_sequence (
    sequence_name VARCHAR(50) PRIMARY KEY,
    next_value INTEGER NOT NULL
);

INSERT INTO history_number_sequence (sequence_name, next_value)
VALUES ('PATIENT_HISTORY', 1);

CREATE TABLE patient (
    id INTEGER NOT NULL AUTO_INCREMENT,
    history_number VARCHAR(20) NOT NULL,
    dni VARCHAR(8) NOT NULL,
    first_names VARCHAR(100) NOT NULL,
    last_names VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,
    sex VARCHAR(1) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(150),
    address VARCHAR(250),
    created_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_patient PRIMARY KEY (id),
    CONSTRAINT uq_patient_history_number UNIQUE (history_number),
    CONSTRAINT uq_patient_dni UNIQUE (dni),
    CONSTRAINT chk_patient_sex CHECK (sex IN ('M', 'F'))
);

CREATE INDEX idx_patient_last_names ON patient (last_names);
