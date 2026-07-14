package pe.com.cpp.scheduling.api;

import java.time.LocalDate;
import java.util.List;

public record PsychologyAgendaResponse(
        Integer psicologoId,
        String identitySubject,
        String nombreCompleto,
        LocalDate fecha,
        List<AppointmentResponse> citas) {
}
