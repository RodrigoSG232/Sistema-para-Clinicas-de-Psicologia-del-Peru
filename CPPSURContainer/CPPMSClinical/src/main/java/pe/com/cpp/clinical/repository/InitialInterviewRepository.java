package pe.com.cpp.clinical.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.cpp.clinical.domain.InitialInterview;
public interface InitialInterviewRepository extends JpaRepository<InitialInterview,Integer>{Optional<InitialInterview> findByProcessId(Integer processId);}
