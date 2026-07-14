package pe.com.cpp.queue.api;
import java.time.*; import pe.com.cpp.queue.domain.TicketStatus;
public record TicketResponse(Long id,String number,LocalDate operationalDate,TicketStatus status,LocalDateTime createdAt,LocalDateTime calledAt,LocalDateTime finishedAt,Integer appointmentId,Integer patientId){}
