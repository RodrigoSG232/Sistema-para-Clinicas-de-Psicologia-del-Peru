package com.clinica.psicologia.repository;
import com.clinica.psicologia.entity.ComprobantePago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ComprobantePagoRepository extends JpaRepository<ComprobantePago, Integer> {
    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_comprobante, 3) AS INTEGER)), 0) FROM \"ComprobantePago\"", nativeQuery = true)
    Integer getMaxCorrelativo();
}