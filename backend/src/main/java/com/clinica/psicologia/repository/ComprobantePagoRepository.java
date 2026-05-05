package com.clinica.psicologia.repository;
import com.clinica.psicologia.entity.ComprobantePago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
public interface ComprobantePagoRepository extends JpaRepository<ComprobantePago, Integer> {
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(c.numeroComprobante,3,LEN(c.numeroComprobante)) AS int)),0) FROM ComprobantePago c")
    Integer getMaxCorrelativo();
}
