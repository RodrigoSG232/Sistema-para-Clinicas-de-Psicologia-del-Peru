package pe.com.cpp.clinical.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.cpp.clinical.domain.ClinicalSession;
public interface ClinicalSessionRepository extends JpaRepository<ClinicalSession,Integer>{boolean existsByAppointmentId(Integer appointmentId);Optional<ClinicalSession> findByAppointmentId(Integer appointmentId);long countByProcessId(Integer processId);List<ClinicalSession> findByProcessIdOrderByRegisteredAtAscIdAsc(Integer processId);}
