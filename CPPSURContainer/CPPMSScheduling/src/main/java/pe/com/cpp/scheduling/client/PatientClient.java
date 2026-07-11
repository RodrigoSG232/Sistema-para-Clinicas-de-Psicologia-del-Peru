package pe.com.cpp.scheduling.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import pe.com.cpp.scheduling.exception.ResourceNotFoundException;
import pe.com.cpp.scheduling.exception.ServiceUnavailableException;

@Component
public class PatientClient {

    private final RestClient restClient;

    public PatientClient(RestClient.Builder loadBalancedRestClientBuilder) {
        this.restClient = loadBalancedRestClientBuilder
                .baseUrl("http://cpp-patient-service/api/patients")
                .build();
    }

    public PatientSnapshot findById(Integer patientId) {
        try {
            PatientSnapshot patient = restClient.get()
                    .uri("/{id}", patientId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ResourceNotFoundException("Paciente no encontrado");
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new ServiceUnavailableException("No fue posible validar el paciente");
                    })
                    .body(PatientSnapshot.class);
            if (patient == null) {
                throw new ServiceUnavailableException("El servicio de pacientes devolvió una respuesta vacía");
            }
            return patient;
        } catch (ResourceNotFoundException | ServiceUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ServiceUnavailableException("El servicio de pacientes no está disponible", exception);
        }
    }
}
