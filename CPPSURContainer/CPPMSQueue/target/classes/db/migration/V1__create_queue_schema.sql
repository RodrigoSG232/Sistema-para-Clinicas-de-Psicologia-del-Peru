CREATE TABLE daily_ticket_sequence (
 operational_date DATE PRIMARY KEY,
 next_value INT NOT NULL,
 active_ticket_id BIGINT NULL,
 CONSTRAINT chk_sequence_next CHECK (next_value >= 1)
);
CREATE TABLE queue_ticket (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 number VARCHAR(10) NOT NULL,
 operational_date DATE NOT NULL,
 status VARCHAR(20) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 called_at DATETIME(6) NULL,
 finished_at DATETIME(6) NULL,
 CONSTRAINT uq_ticket_date_number UNIQUE (operational_date, number),
 CONSTRAINT chk_ticket_status CHECK (status IN ('ESPERA','EN_ATENCION','FINALIZADO')),
 CONSTRAINT chk_ticket_times CHECK ((called_at IS NULL OR called_at >= created_at) AND (finished_at IS NULL OR (called_at IS NOT NULL AND finished_at >= called_at)))
);
CREATE INDEX idx_ticket_date_status_created ON queue_ticket (operational_date, status, created_at);
ALTER TABLE daily_ticket_sequence ADD CONSTRAINT fk_sequence_active_ticket FOREIGN KEY (active_ticket_id) REFERENCES queue_ticket(id);
