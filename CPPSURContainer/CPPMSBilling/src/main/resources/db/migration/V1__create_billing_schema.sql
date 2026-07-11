CREATE TABLE receipt_sequence (
    sequence_name VARCHAR(50) NOT NULL,
    next_value INTEGER NOT NULL,
    CONSTRAINT pk_receipt_sequence PRIMARY KEY (sequence_name),
    CONSTRAINT chk_receipt_sequence_value CHECK (next_value > 0)
);

INSERT INTO receipt_sequence (sequence_name, next_value)
VALUES ('PAYMENT_RECEIPT', 1);

CREATE TABLE debt (
    id INTEGER IDENTITY(1,1) NOT NULL,
    patient_id INTEGER NOT NULL,
    patient_name VARCHAR(201) NOT NULL,
    patient_dni VARCHAR(8) NOT NULL,
    patient_history_number VARCHAR(20) NOT NULL,
    appointment_id INTEGER NULL,
    concept VARCHAR(100) NOT NULL,
    specialty_name VARCHAR(100) NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME2(6) NOT NULL,
    CONSTRAINT pk_debt PRIMARY KEY (id),
    CONSTRAINT uq_debt_appointment UNIQUE (appointment_id),
    CONSTRAINT chk_debt_amount CHECK (amount > 0),
    CONSTRAINT chk_debt_status CHECK (status IN ('PENDING', 'PAID'))
);

CREATE INDEX idx_debt_patient_status ON debt (patient_id, status, created_at);
CREATE INDEX idx_debt_patient_dni ON debt (patient_dni);

CREATE TABLE payment_receipt (
    id INTEGER IDENTITY(1,1) NOT NULL,
    receipt_number VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL,
    debt_id INTEGER NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    amount_paid DECIMAL(10,2) NOT NULL,
    issued_at DATETIME2(6) NOT NULL,
    cashier_username VARCHAR(100) NULL,
    CONSTRAINT pk_payment_receipt PRIMARY KEY (id),
    CONSTRAINT uq_payment_receipt_number UNIQUE (receipt_number),
    CONSTRAINT uq_payment_receipt_debt UNIQUE (debt_id),
    CONSTRAINT fk_payment_receipt_debt FOREIGN KEY (debt_id) REFERENCES debt (id),
    CONSTRAINT chk_payment_receipt_type CHECK (type IN ('RECEIPT', 'INVOICE')),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('CASH', 'CARD', 'YAPE_PLIN')),
    CONSTRAINT chk_payment_amount CHECK (amount_paid > 0)
);
