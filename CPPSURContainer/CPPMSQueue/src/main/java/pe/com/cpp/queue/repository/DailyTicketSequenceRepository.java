package pe.com.cpp.queue.repository;
import java.time.LocalDate; import java.util.Optional; import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import pe.com.cpp.queue.domain.DailyTicketSequence;
public interface DailyTicketSequenceRepository extends JpaRepository<DailyTicketSequence,LocalDate>{
 @Modifying @Query(value="INSERT IGNORE INTO daily_ticket_sequence (operational_date,next_value,active_ticket_id) VALUES (:date,1,NULL)",nativeQuery=true) void ensureExists(@Param("date")LocalDate date);
 @Modifying(flushAutomatically=true) @Query(value="INSERT INTO daily_ticket_sequence (operational_date,next_value,active_ticket_id) VALUES (:date,2,NULL) ON DUPLICATE KEY UPDATE next_value=next_value+1",nativeQuery=true) void advanceNextValue(@Param("date")LocalDate date);
 @Query(value="SELECT next_value FROM daily_ticket_sequence WHERE operational_date=:date",nativeQuery=true) int currentNextValue(@Param("date")LocalDate date);
 default int allocateNext(LocalDate date){advanceNextValue(date);return currentNextValue(date)-1;}
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from DailyTicketSequence s where s.operationalDate=:date") Optional<DailyTicketSequence> findByDateForUpdate(@Param("date")LocalDate date);
}
