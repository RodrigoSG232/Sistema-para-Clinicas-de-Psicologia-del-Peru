package pe.com.cpp.clinical.repository;
import java.util.List; import org.springframework.data.jpa.repository.JpaRepository; import pe.com.cpp.clinical.domain.ClinicalHypothesis;
public interface ClinicalHypothesisRepository extends JpaRepository<ClinicalHypothesis,Integer>{List<ClinicalHypothesis> findByProcessIdOrderByRegisteredAtDesc(Integer processId);}
