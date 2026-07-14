package pe.com.cpp.clinical.domain;
import java.time.LocalDateTime;
import jakarta.persistence.*;
@Entity @Table(name="clinical_session")
public class ClinicalSession {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="therapeutic_process_id",nullable=false) private TherapeuticProcess process;
 @Column(name="appointment_id",nullable=false,updatable=false) private Integer appointmentId;
 @Column(name="session_phase",nullable=false,updatable=false) private Integer sessionPhase;
 @Column(nullable=false,columnDefinition="TEXT",updatable=false) private String evolution;
 @Column(name="patient_indications",columnDefinition="TEXT",updatable=false) private String patientIndications;
 @Column(name="registered_by",nullable=false,updatable=false) private String registeredBy;
 @Column(name="registered_at",nullable=false,updatable=false) private LocalDateTime registeredAt;
 protected ClinicalSession(){}
 public ClinicalSession(TherapeuticProcess process,Integer appointmentId,Integer phase,String evolution,String indications,String registeredBy,LocalDateTime at){this.process=process;this.appointmentId=appointmentId;this.sessionPhase=phase;this.evolution=evolution;this.patientIndications=indications;this.registeredBy=registeredBy;this.registeredAt=at;}
 public Integer getId(){return id;} public Integer getAppointmentId(){return appointmentId;} public Integer getSessionPhase(){return sessionPhase;} public String getEvolution(){return evolution;} public String getPatientIndications(){return patientIndications;} public String getRegisteredBy(){return registeredBy;} public LocalDateTime getRegisteredAt(){return registeredAt;} public TherapeuticProcess getProcess(){return process;}
}
