package com.clinica.psicologia.repository;

import com.clinica.psicologia.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    List<Ticket> findByEstadoOrderByCreadoEnAsc(String estado);

    Optional<Ticket> findFirstByEstadoOrderByCreadoEnAsc(String estado);

    boolean existsByEstado(String estado);

    List<Ticket> findByFechaOrderByCreadoEnAsc(LocalDate fecha);

    List<Ticket> findByFechaAndEstadoOrderByCreadoEnAsc(LocalDate fecha, String estado);

    Optional<Ticket> findFirstByFechaAndEstadoOrderByCreadoEnAsc(LocalDate fecha, String estado);

    boolean existsByFechaAndEstado(LocalDate fecha, String estado);

    @Query(value = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(numero FROM 3) AS INTEGER)), 0)
            FROM ticket
            WHERE fecha = :fecha
            """, nativeQuery = true)
    Integer getMaxCorrelativoPorFecha(@Param("fecha") LocalDate fecha);
}
