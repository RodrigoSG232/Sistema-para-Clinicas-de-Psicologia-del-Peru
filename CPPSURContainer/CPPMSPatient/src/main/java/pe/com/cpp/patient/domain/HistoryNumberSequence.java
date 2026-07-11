package pe.com.cpp.patient.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "history_number_sequence")
public class HistoryNumberSequence {

    public static final String PATIENT_SEQUENCE = "PATIENT_HISTORY";

    @Id
    @Column(name = "sequence_name", length = 50)
    private String name;

    @Column(name = "next_value", nullable = false)
    private Integer nextValue;

    protected HistoryNumberSequence() {
    }

    public Integer takeNextValue() {
        int currentValue = nextValue;
        nextValue = currentValue + 1;
        return currentValue;
    }
}
