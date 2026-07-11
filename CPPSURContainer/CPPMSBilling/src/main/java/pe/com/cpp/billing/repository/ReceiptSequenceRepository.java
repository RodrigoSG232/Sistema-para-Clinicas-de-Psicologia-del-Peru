package pe.com.cpp.billing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import pe.com.cpp.billing.domain.ReceiptSequence;

public interface ReceiptSequenceRepository extends JpaRepository<ReceiptSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ReceiptSequence s where s.name = :name")
    Optional<ReceiptSequence> findByNameForUpdate(@Param("name") String name);
}
