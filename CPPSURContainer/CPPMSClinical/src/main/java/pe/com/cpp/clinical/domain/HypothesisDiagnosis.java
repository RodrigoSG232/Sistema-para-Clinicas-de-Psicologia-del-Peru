package pe.com.cpp.clinical.domain;
import jakarta.persistence.*;
@Entity @Table(name="hypothesis_diagnosis",uniqueConstraints=@UniqueConstraint(name="uq_hypothesis_diagnosis",columnNames={"clinical_hypothesis_id","diagnosis_code"})) public class HypothesisDiagnosis {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="clinical_hypothesis_id",nullable=false,updatable=false) private ClinicalHypothesis hypothesis;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="diagnosis_code",nullable=false,updatable=false) private Cie10Diagnosis diagnosis;
 protected HypothesisDiagnosis(){} public HypothesisDiagnosis(ClinicalHypothesis hypothesis,Cie10Diagnosis diagnosis){this.hypothesis=hypothesis;this.diagnosis=diagnosis;}
 public Cie10Diagnosis getDiagnosis(){return diagnosis;}
}
