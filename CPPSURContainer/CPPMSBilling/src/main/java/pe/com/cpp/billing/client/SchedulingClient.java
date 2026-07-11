package pe.com.cpp.billing.client;

import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import pe.com.cpp.billing.exception.ExternalServiceException;
import pe.com.cpp.billing.exception.ResourceNotFoundException;
import pe.com.cpp.billing.exception.SchedulingUnavailableException;

@Component
public class SchedulingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulingClient.class);

    private final RestClient restClient;

    public SchedulingClient(RestClient.Builder loadBalancedRestClientBuilder) {
        this.restClient = loadBalancedRestClientBuilder
                .baseUrl("http://cpp-scheduling-service/api/scheduling/appointments")
                .build();
    }

    public AppointmentSnapshot findById(Integer appointmentId) {
        return withAvailabilityRetry(() -> findByIdOnce(appointmentId), "consultar", appointmentId);
    }

    private AppointmentSnapshot findByIdOnce(Integer appointmentId) {
        try {
            AppointmentSnapshot appointment = restClient.get()
                    .uri("/{id}", appointmentId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        LOGGER.warn("Agenda devolvió HTTP 404 al consultar la cita {}", appointmentId);
                        throw new ResourceNotFoundException("Cita no encontrada");
                    })
                    .onStatus(status -> status.isError(), (request, response) -> {
                        LOGGER.error("Agenda devolvió HTTP {} al consultar la cita {}",
                                response.getStatusCode().value(), appointmentId);
                        throw new ExternalServiceException("No fue posible consultar la cita");
                    })
                    .body(AppointmentSnapshot.class);
            if (appointment == null) {
                throw new ExternalServiceException("Agenda devolvió una respuesta vacía");
            }
            return appointment;
        } catch (ResourceNotFoundException | ExternalServiceException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            LOGGER.error("Error al consultar la cita {} en Agenda", appointmentId, exception);
            throw new SchedulingUnavailableException("El servicio de agenda no está disponible", exception);
        }
    }

    public void markAsPaid(Integer appointmentId) {
        withAvailabilityRetry(() -> {
            markAsPaidOnce(appointmentId);
            return null;
        }, "actualizar", appointmentId);
    }

    private void markAsPaidOnce(Integer appointmentId) {
        try {
            restClient.patch()
                    .uri("/{id}/status", appointmentId)
                    .body(Map.of("estado", "PAGADA"))
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, response) -> {
                        LOGGER.error("Agenda devolvió HTTP {} al marcar como pagada la cita {}",
                                response.getStatusCode().value(), appointmentId);
                        throw new ExternalServiceException("Agenda no aceptó el pago de la cita");
                    })
                    .toBodilessEntity();
        } catch (ExternalServiceException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            LOGGER.error("Error al marcar como pagada la cita {} en Agenda", appointmentId, exception);
            throw new SchedulingUnavailableException("El servicio de agenda no está disponible", exception);
        }
    }

    private <T> T withAvailabilityRetry(Supplier<T> operation, String action, Integer appointmentId) {
        SchedulingUnavailableException lastException = null;
        for (int attempt = 1; attempt <= 4; attempt++) {
            try {
                return operation.get();
            } catch (SchedulingUnavailableException exception) {
                lastException = exception;
                if (attempt == 4) {
                    break;
                }
                long delayMillis = attempt * 500L;
                LOGGER.warn("Agenda aún no está disponible para {} la cita {}. Reintento {}/4 en {} ms",
                        action, appointmentId, attempt + 1, delayMillis);
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new SchedulingUnavailableException(
                            "Se interrumpió la comunicación con Agenda", interruptedException);
                }
            }
        }
        throw lastException;
    }
}
