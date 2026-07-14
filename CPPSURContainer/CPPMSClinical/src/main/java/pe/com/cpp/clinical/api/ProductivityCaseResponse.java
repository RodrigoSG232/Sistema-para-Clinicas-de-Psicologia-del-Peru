package pe.com.cpp.clinical.api;
import java.time.LocalDateTime;
public record ProductivityCaseResponse(Integer reportId,String ticketNumber,String patientName,String patientHistoryNumber,String psychologistName,LocalDateTime ticketIssuedAt,LocalDateTime dischargedAt,double efficiencyHours){}
