package pe.com.cpp.clinical.domain;
import jakarta.persistence.*;
@Entity @Table(name="cie10_diagnosis") public class Cie10Diagnosis {
 @Id @Column(length=10) private String code;
 @Column(nullable=false,length=300) private String description;
 @Column(nullable=false) private boolean active;
 protected Cie10Diagnosis(){}
 public String getCode(){return code;} public String getDescription(){return description;} public boolean isActive(){return active;}
}
