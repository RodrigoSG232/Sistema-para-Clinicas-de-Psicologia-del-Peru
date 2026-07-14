package pe.com.cpp.clinical.domain;
import java.time.LocalDateTime; import jakarta.persistence.*;
@Entity @Table(name="clinical_discharge_report") public class ClinicalDischargeReport {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="therapeutic_process_id",nullable=false,unique=true,updatable=false) private TherapeuticProcess process;
 @Column(name="discharge_reason",nullable=false,columnDefinition="TEXT",updatable=false) private String dischargeReason;
 @Column(name="treatment_summary",nullable=false,columnDefinition="TEXT",updatable=false) private String treatmentSummary;
 @Column(nullable=false,columnDefinition="TEXT",updatable=false) private String achievements;
 @Column(nullable=false,columnDefinition="TEXT",updatable=false) private String recommendations;
 @Column(name="registered_by",nullable=false,updatable=false) private String registeredBy;
 @Column(name="discharged_at",nullable=false,updatable=false) private LocalDateTime dischargedAt;
 @Column(name="ticket_number",length=10,updatable=false) private String ticketNumber;
 @Column(name="ticket_issued_at",updatable=false) private LocalDateTime ticketIssuedAt;
 protected ClinicalDischargeReport(){}
 public ClinicalDischargeReport(TherapeuticProcess process,String reason,String summary,String achievements,String recommendations,String registeredBy,LocalDateTime at){this.process=process;this.dischargeReason=reason;this.treatmentSummary=summary;this.achievements=achievements;this.recommendations=recommendations;this.registeredBy=registeredBy;this.dischargedAt=at;}
 public ClinicalDischargeReport(TherapeuticProcess process,String reason,String summary,String achievements,String recommendations,String registeredBy,LocalDateTime at,String ticketNumber,LocalDateTime ticketIssuedAt){this(process,reason,summary,achievements,recommendations,registeredBy,at);this.ticketNumber=ticketNumber;this.ticketIssuedAt=ticketIssuedAt;}
 public Integer getId(){return id;} public TherapeuticProcess getProcess(){return process;} public String getDischargeReason(){return dischargeReason;} public String getTreatmentSummary(){return treatmentSummary;} public String getAchievements(){return achievements;} public String getRecommendations(){return recommendations;} public String getRegisteredBy(){return registeredBy;} public LocalDateTime getDischargedAt(){return dischargedAt;} public String getTicketNumber(){return ticketNumber;} public LocalDateTime getTicketIssuedAt(){return ticketIssuedAt;}
}
