package com.clinica.psicologia.repository;
import com.clinica.psicologia.entity.ComprobantePago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ComprobantePagoRepository extends JpaRepository<ComprobantePago, Integer> {
    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_serie FROM 3) AS INTEGER)), 0) FROM comprobantepago", nativeQuery = true)
    Integer getMaxCorrelativo();

    @Query("SELECT c FROM ComprobantePago c LEFT JOIN FETCH c.deuda d LEFT JOIN FETCH d.paciente LEFT JOIN FETCH d.cita ci LEFT JOIN FETCH ci.especialidad WHERE c.id = :id")
    Optional<ComprobantePago> findByIdConDetalle(@Param("id") Integer id);
}
