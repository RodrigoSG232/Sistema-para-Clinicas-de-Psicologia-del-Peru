package com.clinica.psicologia.repository;
import com.clinica.psicologia.entity.Psicologo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PsicologoRepository extends JpaRepository<Psicologo, Integer> {
    List<Psicologo> findByActivoTrue();

    @Query("SELECT p FROM Psicologo p WHERE p.especialidad.id = :espId AND p.activo = true")
    List<Psicologo> findByEspecialidadId(@Param("espId") Integer espId);

    Optional<Psicologo> findByUsuarioId(Integer usuarioId);
}
