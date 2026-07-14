package pe.com.cpp.clinical.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExternalProcessCreateRequest(
        @NotNull Integer appointmentId,
        @NotNull Integer psychologistId,
        @NotBlank String psychologistName,
        @NotBlank String patientName,
        @NotBlank String patientDni,
        @NotBlank String patientHistoryNumber,
        String observaciones,
        @NotNull @Valid InitialInterviewRequest entrevista) {
}
