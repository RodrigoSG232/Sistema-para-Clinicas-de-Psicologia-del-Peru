package pe.com.cpp.queue.domain;
import java.time.*; import jakarta.persistence.*;
@Entity @Table(name="queue_ticket",uniqueConstraints=@UniqueConstraint(name="uq_ticket_date_number",columnNames={"operational_date","number"}))
public class QueueTicket {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=10) private String number;
 @Column(name="operational_date",nullable=false) private LocalDate operationalDate;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private TicketStatus status;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @Column(name="called_at") private LocalDateTime calledAt;
 @Column(name="finished_at") private LocalDateTime finishedAt;
 @Column(name="appointment_id",unique=true) private Integer appointmentId;
 @Column(name="patient_id") private Integer patientId;
 protected QueueTicket(){}
 public QueueTicket(String number,LocalDate date,LocalDateTime createdAt){this.number=number;this.operationalDate=date;this.createdAt=createdAt;this.status=TicketStatus.ESPERA;}
 public QueueTicket(String number,LocalDate date,LocalDateTime createdAt,Integer appointmentId,Integer patientId){this(number,date,createdAt);this.appointmentId=appointmentId;this.patientId=patientId;}
 public void call(LocalDateTime at){if(status!=TicketStatus.ESPERA)throw new IllegalStateException("Solo se puede llamar un ticket en espera");status=TicketStatus.EN_ATENCION;calledAt=at;}
 public void finish(LocalDateTime at){if(status!=TicketStatus.EN_ATENCION)throw new IllegalStateException("Solo se puede finalizar un ticket en atención");status=TicketStatus.FINALIZADO;finishedAt=at;}
 public void attachToAppointment(Integer appointmentId,Integer patientId){if(appointmentId==null||patientId==null)throw new IllegalArgumentException("La cita y el paciente son obligatorios");if(this.appointmentId!=null&&!this.appointmentId.equals(appointmentId))throw new IllegalStateException("El ticket ya está vinculado a otra cita");if(this.patientId!=null&&!this.patientId.equals(patientId))throw new IllegalStateException("El ticket ya está vinculado a otro paciente");this.appointmentId=appointmentId;this.patientId=patientId;}
 public Long getId(){return id;} public String getNumber(){return number;} public LocalDate getOperationalDate(){return operationalDate;} public TicketStatus getStatus(){return status;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getCalledAt(){return calledAt;} public LocalDateTime getFinishedAt(){return finishedAt;} public Integer getAppointmentId(){return appointmentId;} public Integer getPatientId(){return patientId;}
}
