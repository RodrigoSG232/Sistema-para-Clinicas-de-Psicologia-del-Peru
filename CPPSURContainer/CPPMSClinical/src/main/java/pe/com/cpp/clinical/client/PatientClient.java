package pe.com.cpp.clinical.client;
import org.springframework.stereotype.Component; import org.springframework.web.client.RestClient;
import pe.com.cpp.clinical.exception.*;
@Component public class PatientClient {
 private final RestClient client;
 public PatientClient(RestClient.Builder builder){this.client=builder.baseUrl("http://cpp-patient-service/api/patients").build();}
 public PatientSnapshot findById(Integer id){try{PatientSnapshot result=client.get().uri("/{id}",id).retrieve().onStatus(s->s.value()==404,(q,r)->{throw new ResourceNotFoundException("Paciente no encontrado");}).onStatus(s->s.isError(),(q,r)->{throw new ExternalServiceException("No fue posible validar el paciente");}).body(PatientSnapshot.class);if(result==null)throw new ExternalServiceException("Pacientes devolvió una respuesta vacía");return result;}catch(ResourceNotFoundException|ExternalServiceException e){throw e;}catch(RuntimeException e){throw new ExternalServiceException("El servicio de pacientes no está disponible",e);}}
}
