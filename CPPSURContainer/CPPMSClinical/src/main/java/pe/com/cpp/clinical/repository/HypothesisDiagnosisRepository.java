package pe.com.cpp.clinical.repository;
import java.util.List; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import pe.com.cpp.clinical.domain.HypothesisDiagnosis;
public interface HypothesisDiagnosisRepository extends JpaRepository<HypothesisDiagnosis,Integer>{@Query("select a from HypothesisDiagnosis a join fetch a.diagnosis where a.hypothesis.id=:id order by a.diagnosis.code") List<HypothesisDiagnosis> findForHypothesis(@Param("id")Integer hypothesisId);}
