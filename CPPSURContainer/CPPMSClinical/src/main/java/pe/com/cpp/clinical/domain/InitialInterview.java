package pe.com.cpp.clinical.domain;
import java.time.LocalDateTime;
import jakarta.persistence.*;
@Entity @Table(name="initial_interview")
public class InitialInterview {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="therapeutic_process_id",nullable=false,unique=true) private TherapeuticProcess process;
 @Column(name="reason_for_consultation",nullable=false,columnDefinition="TEXT") private String reasonForConsultation;
 @Column(name="personal_history",columnDefinition="TEXT") private String personalHistory;
 @Column(name="family_history",columnDefinition="TEXT") private String familyHistory;
 @Column(name="initial_observations",columnDefinition="TEXT") private String initialObservations;
 @Column(name="registered_at",nullable=false) private LocalDateTime registeredAt;
 protected InitialInterview(){}
 public InitialInterview(TherapeuticProcess process,String reason,String personal,String family,String observations,LocalDateTime at){this.process=process;this.reasonForConsultation=reason;this.personalHistory=personal;this.familyHistory=family;this.initialObservations=observations;this.registeredAt=at;}
 public Integer getId(){return id;} public String getReasonForConsultation(){return reasonForConsultation;} public String getPersonalHistory(){return personalHistory;} public String getFamilyHistory(){return familyHistory;} public String getInitialObservations(){return initialObservations;} public LocalDateTime getRegisteredAt(){return registeredAt;}
}
