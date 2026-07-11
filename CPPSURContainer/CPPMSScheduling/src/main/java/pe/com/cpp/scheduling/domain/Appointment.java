package pe.com.cpp.scheduling.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointment")
public class Appointment {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "psychologist_id", nullable = false)
    private Psychologist psychologist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "appointment_time", nullable = false)
    private LocalTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AppointmentStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Appointment() {
    }

    public Appointment(Integer patientId, String patientName, String patientDni,
            String patientHistoryNumber, Psychologist psychologist, Specialty specialty,
            LocalDate appointmentDate, LocalTime appointmentTime, LocalDateTime createdAt) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientDni = patientDni;
        this.patientHistoryNumber = patientHistoryNumber;
        this.psychologist = psychologist;
        this.specialty = specialty;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.status = AppointmentStatus.PENDING_PAYMENT;
        this.createdAt = createdAt;
    }

    public Integer getId() { return id; }
    public Integer getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public String getPatientDni() { return patientDni; }
    public String getPatientHistoryNumber() { return patientHistoryNumber; }
    public Psychologist getPsychologist() { return psychologist; }
    public Specialty getSpecialty() { return specialty; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public LocalTime getAppointmentTime() { return appointmentTime; }
    public AppointmentStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void changeStatus(AppointmentStatus newStatus) {
        this.status = newStatus;
    }
}
