package com.clinica.psicologia.repository;
import com.clinica.psicologia.entity.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface SesionRepository extends JpaRepository<Sesion, Integer> {
    @Query("SELECT s FROM Sesion s LEFT JOIN FETCH s.registradoPor WHERE s.procesoTerapeutico.id = :procesoId ORDER BY s.fechaRegistro DESC")
    List<Sesion> findByProcesoTerapeuticoIdOrderByFechaRegistroDesc(@Param("procesoId") Integer procesoId);
    List<Sesion> findByCitaId(Integer citaId);
}
