package pe.com.cpp.billing.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReceiptResponse(
        Integer id,
        String numeroComprobante,
        String tipo,
        String medioPago,
        BigDecimal montoPagado,
        LocalDateTime fechaPago,
        String cajero,
        Integer deudaId,
        String concepto,
        String estadoDeuda,
        Integer pacienteId,
        String pacienteNombre,
        String pacienteDni,
        String especialidad,
        Integer citaId) {
}
