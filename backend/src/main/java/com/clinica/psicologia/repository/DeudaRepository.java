package com.clinica.psicologia.repository;
import com.clinica.psicologia.entity.Deuda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DeudaRepository extends JpaRepository<Deuda, Integer> {

    @Query("SELECT d FROM Deuda d LEFT JOIN FETCH d.paciente LEFT JOIN FETCH d.cita c LEFT JOIN FETCH c.especialidad LEFT JOIN FETCH c.ticket WHERE d.paciente.id = :pacienteId AND d.estado = 'PENDIENTE' AND (:concepto IS NULL OR d.concepto LIKE CONCAT('%',:concepto,'%'))")
    List<Deuda> buscarPendientes(@Param("pacienteId") Integer pacienteId, @Param("concepto") String concepto);

    @Query("SELECT d FROM Deuda d LEFT JOIN FETCH d.paciente LEFT JOIN FETCH d.cita c LEFT JOIN FETCH c.especialidad LEFT JOIN FETCH c.ticket WHERE d.estado = 'PENDIENTE' AND (LOWER(d.paciente.nombres) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(d.paciente.apellidos) LIKE LOWER(CONCAT('%',:q,'%')) OR d.paciente.dni LIKE CONCAT('%',:q,'%'))")
    List<Deuda> buscarPorPaciente(@Param("q") String q);

    @Query("SELECT d FROM Deuda d LEFT JOIN FETCH d.paciente LEFT JOIN FETCH d.cita c LEFT JOIN FETCH c.especialidad LEFT JOIN FETCH c.ticket WHERE d.estado = :estado")
    List<Deuda> findByEstado(@Param("estado") String estado);
}