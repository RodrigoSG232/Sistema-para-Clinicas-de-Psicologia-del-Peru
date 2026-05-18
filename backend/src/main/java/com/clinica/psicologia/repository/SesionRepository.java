package com.clinica.psicologia.repository;
import com.clinica.psicologia.entity.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SesionRepository extends JpaRepository<Sesion, Integer> {
    List<Sesion> findByProcesoTerapeuticoIdOrderByFechaRegistroDesc(Integer procesoId);
    List<Sesion> findByCitaId(Integer citaId);
}
