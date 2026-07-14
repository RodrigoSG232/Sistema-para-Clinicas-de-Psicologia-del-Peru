package pe.com.cpp.clinical.client;
import org.springframework.stereotype.Component; import org.springframework.web.client.RestClient; import pe.com.cpp.clinical.exception.*;
@Component public class QueueClient {
 private final RestClient client;
 public QueueClient(RestClient.Builder builder){this.client=builder.baseUrl("http://cpp-queue-service/api/queue/tickets").build();}
 public QueueTicketSnapshot findByAppointment(Integer appointmentId){try{QueueTicketSnapshot result=client.get().uri("/appointments/{id}",appointmentId).retrieve().onStatus(s->s.value()==404,(q,r)->{throw new ResourceNotFoundException("La cita no tiene ticket emitido");}).onStatus(s->s.isError(),(q,r)->{throw new ExternalServiceException("No fue posible consultar el ticket");}).body(QueueTicketSnapshot.class);if(result==null)throw new ExternalServiceException("Turnos devolvió una respuesta vacía");return result;}catch(ResourceNotFoundException|ExternalServiceException e){throw e;}catch(RuntimeException e){throw new ExternalServiceException("El servicio de turnos no está disponible",e);}}
}
