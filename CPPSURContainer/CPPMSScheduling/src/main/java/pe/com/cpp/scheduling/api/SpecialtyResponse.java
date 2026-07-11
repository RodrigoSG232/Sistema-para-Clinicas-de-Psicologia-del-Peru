package pe.com.cpp.scheduling.api;

import java.math.BigDecimal;

public record SpecialtyResponse(
        Integer id,
        String nombre,
        String descripcion,
        BigDecimal tarifa) {
}
