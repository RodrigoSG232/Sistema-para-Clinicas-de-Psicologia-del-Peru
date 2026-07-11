package pe.com.cpp.scheduling.api;

import jakarta.validation.constraints.NotBlank;

public record AppointmentStatusRequest(
        @NotBlank(message = "El estado es obligatorio") String estado) {
}
