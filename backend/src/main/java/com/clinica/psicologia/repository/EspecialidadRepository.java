package com.clinica.psicologia.repository;
import com.clinica.psicologia.entity.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Integer> {
    List<Especialidad> findByActivoTrue();
}