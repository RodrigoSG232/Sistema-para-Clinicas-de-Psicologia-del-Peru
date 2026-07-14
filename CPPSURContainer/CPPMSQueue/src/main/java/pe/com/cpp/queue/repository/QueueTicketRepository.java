package pe.com.cpp.queue.repository;
import java.time.LocalDate; import java.util.*; import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import pe.com.cpp.queue.domain.*;
public interface QueueTicketRepository extends JpaRepository<QueueTicket,Long>{
 List<QueueTicket> findByOperationalDateOrderByCreatedAtAsc(LocalDate date); List<QueueTicket> findByOperationalDateAndStatusOrderByCreatedAtAsc(LocalDate date,TicketStatus status);
 Optional<QueueTicket> findFirstByOperationalDateAndStatusOrderByCreatedAtAsc(LocalDate date,TicketStatus status);
 Optional<QueueTicket> findByAppointmentId(Integer appointmentId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from QueueTicket t where t.id=:id") Optional<QueueTicket> findByIdForUpdate(@Param("id")Long id);
}
