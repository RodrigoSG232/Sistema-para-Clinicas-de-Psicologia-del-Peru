package pe.com.cpp.clinical.domain;

import java.time.LocalDate;
import jakarta.persistence.*;

@Entity @Table(name="therapeutic_process")
public class TherapeuticProcess {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @Column(name="patient_id",nullable=false) private Integer patientId;
    @Column(name="patient_name",nullable=false) private String patientName;
    @Column(name="patient_dni",nullable=false,length=8) private String patientDni;
    @Column(name="patient_history_number",nullable=false) private String patientHistoryNumber;
    @Column(name="psychologist_id",nullable=false) private Integer psychologistId;
    @Column(name="psychologist_name",nullable=false) private String psychologistName;
    @Column(name="current_phase",nullable=false) private Integer currentPhase;
    @Column(name="start_date",nullable=false) private LocalDate startDate;
    @Column(name="end_date") private LocalDate endDate;
    @Column(columnDefinition="TEXT") private String observations;
    @Column(nullable=false) private boolean active;
    @Column(nullable=false,length=20) private String status;
    @Column(name="initial_appointment_id") private Integer initialAppointmentId;
    protected TherapeuticProcess() {}
    public TherapeuticProcess(Integer patientId,String patientName,String patientDni,String patientHistoryNumber,Integer psychologistId,String psychologistName,LocalDate startDate,String observations){this.patientId=patientId;this.patientName=patientName;this.patientDni=patientDni;this.patientHistoryNumber=patientHistoryNumber;this.psychologistId=psychologistId;this.psychologistName=psychologistName;this.currentPhase=1;this.startDate=startDate;this.observations=observations;this.active=true;this.status="ACTIVO";}
    public TherapeuticProcess(Integer patientId,String patientName,String patientDni,String patientHistoryNumber,Integer psychologistId,String psychologistName,LocalDate startDate,String observations,Integer initialAppointmentId){this(patientId,patientName,patientDni,patientHistoryNumber,psychologistId,psychologistName,startDate,observations);this.initialAppointmentId=initialAppointmentId;}
    public void changePhase(int phase,String observations){if(!active)throw new IllegalStateException("La historia clínica está cerrada por alta");this.currentPhase=phase;if(observations!=null)this.observations=observations;}
    public void discharge(LocalDate date){if(!active)throw new IllegalStateException("El proceso ya tiene alta");if(currentPhase!=4)throw new IllegalStateException("El alta solo puede registrarse en Fase 4");this.endDate=date;this.active=false;this.status="ALTA";}
    public Integer getId(){return id;} public Integer getPatientId(){return patientId;} public String getPatientName(){return patientName;} public String getPatientDni(){return patientDni;} public String getPatientHistoryNumber(){return patientHistoryNumber;} public Integer getPsychologistId(){return psychologistId;} public String getPsychologistName(){return psychologistName;} public Integer getCurrentPhase(){return currentPhase;} public LocalDate getStartDate(){return startDate;} public LocalDate getEndDate(){return endDate;} public String getObservations(){return observations;} public boolean isActive(){return active;} public String getStatus(){return status;} public Integer getInitialAppointmentId(){return initialAppointmentId;}
}
