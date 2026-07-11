package pe.com.cpp.patient.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "patient")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "history_number", nullable = false, unique = true, length = 20)
    private String historyNumber;

    @Column(nullable = false, unique = true, length = 8)
    private String dni;

    @Column(nullable = false, length = 100)
    private String firstNames;

    @Column(nullable = false, length = 100)
    private String lastNames;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 1)
    private String sex;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(length = 250)
    private String address;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean active;

    protected Patient() {
    }

    public Patient(String historyNumber, String dni, String firstNames, String lastNames,
            LocalDate birthDate, String sex, String phone, String email, String address,
            LocalDateTime createdAt) {
        this.historyNumber = historyNumber;
        this.dni = dni;
        this.firstNames = firstNames;
        this.lastNames = lastNames;
        this.birthDate = birthDate;
        this.sex = sex;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.createdAt = createdAt;
        this.active = true;
    }

    public Integer getId() {
        return id;
    }

    public String getHistoryNumber() {
        return historyNumber;
    }

    public String getDni() {
        return dni;
    }

    public String getFirstNames() {
        return firstNames;
    }

    public String getLastNames() {
        return lastNames;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getSex() {
        return sex;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }
}
