package com.clinica.psicologia.repository;
import com.clinica.psicologia.entity.ProcesoTerapeutico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ProcesoTerapeuticoRepository extends JpaRepository<ProcesoTerapeutico, Integer> {
    Optional<ProcesoTerapeutico> findByPacienteIdAndActivoTrue(Integer pacienteId);
    List<ProcesoTerapeutico> findByPsicologoIdAndActivoTrue(Integer psicologoId);
}
