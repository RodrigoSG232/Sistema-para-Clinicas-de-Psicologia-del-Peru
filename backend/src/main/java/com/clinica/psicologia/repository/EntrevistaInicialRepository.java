package com.clinica.psicologia.repository;
import com.clinica.psicologia.entity.EntrevistaInicial;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface EntrevistaInicialRepository extends JpaRepository<EntrevistaInicial, Integer> {
    Optional<EntrevistaInicial> findByProcesoTerapeuticoId(Integer procesoTerapeuticoId);
}
