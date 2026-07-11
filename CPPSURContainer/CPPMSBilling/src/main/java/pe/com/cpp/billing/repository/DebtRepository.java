package pe.com.cpp.billing.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import pe.com.cpp.billing.domain.Debt;
import pe.com.cpp.billing.domain.DebtStatus;

public interface DebtRepository extends JpaRepository<Debt, Integer> {

    boolean existsByAppointmentId(Integer appointmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Debt d where d.id = :id")
    Optional<Debt> findByIdForUpdate(@Param("id") Integer id);

    @Query("""
            select d from Debt d
            where d.status = :status
              and (:patient is null
                or lower(d.patientName) like lower(concat('%', :patient, '%'))
                or d.patientDni like concat('%', :patient, '%'))
              and (:concept is null or lower(d.concept) like lower(concat('%', :concept, '%')))
            order by d.createdAt
            """)
    List<Debt> search(@Param("status") DebtStatus status,
            @Param("patient") String patient, @Param("concept") String concept);

    List<Debt> findByPatientIdAndStatusOrderByCreatedAt(Integer patientId, DebtStatus status);
}
