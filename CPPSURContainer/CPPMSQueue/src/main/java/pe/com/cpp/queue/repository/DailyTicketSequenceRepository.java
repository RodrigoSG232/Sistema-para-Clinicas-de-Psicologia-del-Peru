package pe.com.cpp.queue.repository;
import java.time.LocalDate; import java.util.Optional; import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import pe.com.cpp.queue.domain.DailyTicketSequence;
public interface DailyTicketSequenceRepository extends JpaRepository<DailyTicketSequence,LocalDate>{
 @Modifying @Query(value="INSERT IGNORE INTO daily_ticket_sequence (operational_date,next_value,active_ticket_id) VALUES (:date,1,NULL)",nativeQuery=true) void ensureExists(@Param("date")LocalDate date);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from DailyTicketSequence s where s.operationalDate=:date") Optional<DailyTicketSequence> findByDateForUpdate(@Param("date")LocalDate date);
}
