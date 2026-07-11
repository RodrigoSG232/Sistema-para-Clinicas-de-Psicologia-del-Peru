package pe.com.cpp.scheduling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "psychologist")
public class Psychologist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "first_names", nullable = false, length = 100)
    private String firstNames;

    @Column(name = "last_names", nullable = false, length = 100)
    private String lastNames;

    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    private String licenseNumber;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    @Column(nullable = false)
    private boolean active;

    protected Psychologist() {
    }

    public Integer getId() {
        return id;
    }

    public String getFirstNames() {
        return firstNames;
    }

    public String getLastNames() {
        return lastNames;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public Specialty getSpecialty() {
        return specialty;
    }

    public boolean isActive() {
        return active;
    }

    public String getFullName() {
        return firstNames + " " + lastNames;
    }
}
