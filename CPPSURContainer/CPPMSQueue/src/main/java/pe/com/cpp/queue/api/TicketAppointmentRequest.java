package pe.com.cpp.queue.api;

import jakarta.validation.constraints.NotNull;

public record TicketAppointmentRequest(
        @NotNull Integer appointmentId,
        @NotNull Integer patientId) {
}
