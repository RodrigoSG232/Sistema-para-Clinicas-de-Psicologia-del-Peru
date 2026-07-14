package pe.com.cpp.clinical.domain;
import java.time.LocalDateTime; import jakarta.persistence.*;
@Entity @Table(name="clinical_hypothesis") public class ClinicalHypothesis {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="therapeutic_process_id",nullable=false) private TherapeuticProcess process;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="clinical_session_id") private ClinicalSession session;
 @Column(nullable=false) private Integer phase;
 @Column(nullable=false,columnDefinition="TEXT",updatable=false) private String hypothesis;
 @Column(name="therapeutic_plan",nullable=false,columnDefinition="TEXT",updatable=false) private String therapeuticPlan;
 @Column(name="registered_by",nullable=false,updatable=false) private String registeredBy;
 @Column(name="registered_at",nullable=false,updatable=false) private LocalDateTime registeredAt;
 protected ClinicalHypothesis(){}
 public ClinicalHypothesis(TherapeuticProcess process,ClinicalSession session,String hypothesis,String plan,String registeredBy,LocalDateTime at){this.process=process;this.session=session;this.phase=2;this.hypothesis=hypothesis;this.therapeuticPlan=plan;this.registeredBy=registeredBy;this.registeredAt=at;}
 public Integer getId(){return id;} public TherapeuticProcess getProcess(){return process;} public ClinicalSession getSession(){return session;} public Integer getPhase(){return phase;} public String getHypothesis(){return hypothesis;} public String getTherapeuticPlan(){return therapeuticPlan;} public String getRegisteredBy(){return registeredBy;} public LocalDateTime getRegisteredAt(){return registeredAt;}
}
