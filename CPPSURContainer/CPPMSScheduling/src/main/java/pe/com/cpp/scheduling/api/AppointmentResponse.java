package pe.com.cpp.scheduling.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AppointmentResponse(
        Integer id,
        LocalDate fecha,
        LocalTime hora,
        String estado,
        Integer psicologoId,
        String psicologo,
        Integer especialidadId,
        String especialidad,
        BigDecimal monto,
        Integer pacienteId,
        String paciente,
        String pacienteDni,
        String pacienteHc,
        LocalDateTime creadoEn) {
}
