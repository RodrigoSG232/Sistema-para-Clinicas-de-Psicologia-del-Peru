package pe.com.cpp.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "receipt_sequence")
public class ReceiptSequence {

    public static final String PAYMENT_RECEIPT_SEQUENCE = "PAYMENT_RECEIPT";

    @Id
    @Column(name = "sequence_name", length = 50)
    private String name;

    @Column(name = "next_value", nullable = false)
    private Integer nextValue;

    protected ReceiptSequence() {
    }

    public Integer takeNextValue() {
        int current = nextValue;
        nextValue = current + 1;
        return current;
    }
}
