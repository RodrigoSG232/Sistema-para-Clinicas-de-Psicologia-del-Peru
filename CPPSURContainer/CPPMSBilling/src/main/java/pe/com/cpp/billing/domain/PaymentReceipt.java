package pe.com.cpp.billing.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_receipt")
public class PaymentReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 20)
    private String receiptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReceiptType type;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debt_id", nullable = false, unique = true)
    private Debt debt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "amount_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "cashier_username", length = 100)
    private String cashierUsername;

    protected PaymentReceipt() {
    }

    public PaymentReceipt(String receiptNumber, ReceiptType type, Debt debt,
            PaymentMethod paymentMethod, BigDecimal amountPaid, LocalDateTime issuedAt,
            String cashierUsername) {
        this.receiptNumber = receiptNumber;
        this.type = type;
        this.debt = debt;
        this.paymentMethod = paymentMethod;
        this.amountPaid = amountPaid;
        this.issuedAt = issuedAt;
        this.cashierUsername = cashierUsername;
    }

    public Integer getId() { return id; }
    public String getReceiptNumber() { return receiptNumber; }
    public ReceiptType getType() { return type; }
    public Debt getDebt() { return debt; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public String getCashierUsername() { return cashierUsername; }
}
