package pe.com.cpp.billing.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DebtResponse(
        Integer id,
        Integer pacienteId,
        String pacienteNombre,
        String pacienteDni,
        String pacienteHc,
        Integer citaId,
        String concepto,
        String especialidad,
        BigDecimal monto,
        String estado,
        LocalDateTime creadoEn) {
}
