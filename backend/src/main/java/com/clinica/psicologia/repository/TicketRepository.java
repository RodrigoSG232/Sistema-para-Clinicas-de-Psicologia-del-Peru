package com.clinica.psicologia.repository;
import com.clinica.psicologia.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    List<Ticket> findByEstadoOrderByFechaEmisionAsc(String estado);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(t.numero,3,LEN(t.numero)) AS int)),0) FROM Ticket t WHERE t.fechaEmision >= :desde")
    Integer getMaxCorrelativoDia(@Param("desde") LocalDateTime desde);
}
