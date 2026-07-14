package pe.com.cpp.clinical.api;
import java.time.LocalDateTime;
public record SessionResponse(Integer id,Integer processId,Integer appointmentId,Integer sessionPhase,String evolution,String patientIndications,String registeredBy,LocalDateTime registeredAt){}
