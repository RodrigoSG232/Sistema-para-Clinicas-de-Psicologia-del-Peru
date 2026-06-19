package com.clinica.psicologia.repository;
import com.clinica.psicologia.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Integer> {
    Optional<Paciente> findByDni(String dni);
    boolean existsByDni(String dni);

    @Query("SELECT p FROM Paciente p WHERE LOWER(p.nombres) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.apellidos) LIKE LOWER(CONCAT('%',:q,'%')) OR p.dni LIKE CONCAT('%',:q,'%')")
    List<Paciente> buscar(@Param("q") String q);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_historia, 4) AS INTEGER)), 0) FROM paciente", nativeQuery = true)
    Integer getMaxNumeroHistoria();
}