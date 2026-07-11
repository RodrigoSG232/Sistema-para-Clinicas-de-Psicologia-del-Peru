package pe.com.cpp.scheduling.api;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(
        Integer psicologoId,
        LocalDate fecha,
        List<String> horasDisponibles,
        List<String> horasOcupadas) {
}
