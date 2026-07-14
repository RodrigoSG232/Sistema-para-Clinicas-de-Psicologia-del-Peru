package pe.com.cpp.clinical.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.cpp.clinical.domain.TherapeuticProcess;
public interface TherapeuticProcessRepository extends JpaRepository<TherapeuticProcess,Integer>{Optional<TherapeuticProcess> findByPatientIdAndActiveTrue(Integer patientId);Optional<TherapeuticProcess> findFirstByPatientIdOrderByStartDateDescIdDesc(Integer patientId);}
