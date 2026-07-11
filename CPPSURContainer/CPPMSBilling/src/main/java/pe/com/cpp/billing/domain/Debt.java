package pe.com.cpp.billing.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "debt")
public class Debt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "patient_id", nullable = false)
    private Integer patientId;

    @Column(name = "patient_name", nullable = false, length = 201)
    private String patientName;

    @Column(name = "patient_dni", nullable = false, length = 8)
    private String patientDni;

    @Column(name = "patient_history_number", nullable = false, length = 20)
    private String patientHistoryNumber;

    @Column(name = "appointment_id", unique = true)
    private Integer appointmentId;

    @Column(nullable = false, length = 100)
    private String concept;

    @Column(name = "specialty_name", length = 100)
    private String specialtyName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DebtStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Debt() {
    }

    public Debt(Integer patientId, String patientName, String patientDni,
            String patientHistoryNumber, Integer appointmentId, String concept,
            String specialtyName, BigDecimal amount, LocalDateTime createdAt) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientDni = patientDni;
        this.patientHistoryNumber = patientHistoryNumber;
        this.appointmentId = appointmentId;
        this.concept = concept;
        this.specialtyName = specialtyName;
        this.amount = amount;
        this.status = DebtStatus.PENDING;
        this.createdAt = createdAt;
    }

    public Integer getId() { return id; }
    public Integer getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public String getPatientDni() { return patientDni; }
    public String getPatientHistoryNumber() { return patientHistoryNumber; }
    public Integer getAppointmentId() { return appointmentId; }
    public String getConcept() { return concept; }
    public String getSpecialtyName() { return specialtyName; }
    public BigDecimal getAmount() { return amount; }
    public DebtStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void markAsPaid() {
        this.status = DebtStatus.PAID;
    }
}
