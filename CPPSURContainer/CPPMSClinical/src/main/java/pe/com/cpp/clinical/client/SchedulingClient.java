package pe.com.cpp.clinical.client;
import java.util.Map; import org.springframework.stereotype.Component; import org.springframework.web.client.RestClient;
import pe.com.cpp.clinical.exception.*;
@Component public class SchedulingClient {
 private final RestClient client;
 public SchedulingClient(RestClient.Builder builder){this.client=builder.baseUrl("http://cpp-scheduling-service/api/scheduling/appointments").build();}
 public AppointmentSnapshot findById(Integer id){return retry(()->{try{AppointmentSnapshot result=client.get().uri("/{id}",id).retrieve().onStatus(s->s.value()==404,(q,r)->{throw new ResourceNotFoundException("Cita no encontrada");}).onStatus(s->s.isError(),(q,r)->{throw new ExternalServiceException("No fue posible validar la cita");}).body(AppointmentSnapshot.class);if(result==null)throw new ExternalServiceException("Agenda devolvió una respuesta vacía");return result;}catch(ResourceNotFoundException|ExternalServiceException e){throw e;}catch(RuntimeException e){throw new TemporaryFailure("Agenda no está disponible",e);}});}
 public void markAttended(Integer id){retry(()->{try{client.patch().uri("/{id}/status",id).body(Map.of("estado","ATENDIDA")).retrieve().onStatus(s->s.isError(),(q,r)->{throw new ExternalServiceException("Agenda no aceptó marcar la cita como atendida");}).toBodilessEntity();return null;}catch(ExternalServiceException e){throw e;}catch(RuntimeException e){throw new TemporaryFailure("Agenda no está disponible",e);}});}
 private <T>T retry(java.util.function.Supplier<T> action){TemporaryFailure last=null;for(int i=0;i<3;i++){try{return action.get();}catch(TemporaryFailure e){last=e;if(i<2)try{Thread.sleep(250L*(i+1));}catch(InterruptedException x){Thread.currentThread().interrupt();throw new ExternalServiceException("Comunicación con Agenda interrumpida",x);}}}throw new ExternalServiceException(last.getMessage(),last);}
 private static class TemporaryFailure extends RuntimeException{TemporaryFailure(String m,Throwable c){super(m,c);}}
}
