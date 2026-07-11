package pe.com.cpp.patient.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import pe.com.cpp.patient.domain.HistoryNumberSequence;

public interface HistoryNumberSequenceRepository extends JpaRepository<HistoryNumberSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from HistoryNumberSequence s where s.name = :name")
    Optional<HistoryNumberSequence> findByNameForUpdate(@Param("name") String name);
}
