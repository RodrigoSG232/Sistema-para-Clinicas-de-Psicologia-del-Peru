package pe.com.cpp.clinical.client;
import java.time.LocalDateTime;
public record QueueTicketSnapshot(Long id,String number,String status,LocalDateTime createdAt,Integer appointmentId,Integer patientId){}
