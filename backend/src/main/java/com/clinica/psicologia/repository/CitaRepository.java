package com.clinica.psicologia.repository;
import com.clinica.psicologia.entity.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Integer> {
    List<Cita> findByPacienteIdOrderByCreadoEnDesc(Integer pacienteId);

    @Query("SELECT c FROM Cita c WHERE c.psicologo.id = :psicId AND c.fechaCita = :fecha ORDER BY c.horaCita")
    List<Cita> findByPsicologoFecha(@Param("psicId") Integer psicId, @Param("fecha") LocalDate fecha);

    @Query("""
    SELECT COUNT(c) > 0
    FROM Cita c
    WHERE c.psicologo.id = :psicologoId
    AND c.fechaCita = :fecha
    AND FUNCTION('FORMAT', c.horaCita, 'HH:mm') = :hora
""")
boolean existeCita(Integer psicologoId, LocalDate fecha, String hora);
    
    @Query("SELECT c FROM Cita c WHERE c.psicologo.id = :psicId AND c.fechaCita = :fecha AND c.estado NOT IN ('CANCELADA','ATENDIDA')")
    List<Cita> findHorasOcupadas(@Param("psicId") Integer psicId, @Param("fecha") LocalDate fecha);
}
