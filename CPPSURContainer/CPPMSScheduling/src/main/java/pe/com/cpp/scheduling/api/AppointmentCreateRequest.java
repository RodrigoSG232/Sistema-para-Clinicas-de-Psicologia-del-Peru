package pe.com.cpp.scheduling.api;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record AppointmentCreateRequest(
        @NotNull(message = "El paciente es obligatorio") Integer pacienteId,
        @NotNull(message = "El psicólogo es obligatorio") Integer psicologoId,
        @NotNull(message = "La especialidad es obligatoria") Integer especialidadId,
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @NotNull(message = "La hora es obligatoria") LocalTime hora) {
}
