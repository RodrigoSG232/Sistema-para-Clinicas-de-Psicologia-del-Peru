package pe.com.cpp.clinical.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExternalSessionCreateRequest(
        @NotNull Integer processId,
        @NotNull Integer appointmentId,
        @Min(1) @Max(4) Integer sessionPhase,
        @NotBlank String evolution,
        String patientIndications,
        @NotBlank String registeredBy) {
}
