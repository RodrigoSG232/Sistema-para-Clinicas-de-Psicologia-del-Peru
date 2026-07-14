package pe.com.cpp.clinical.api;
import java.time.LocalDate; import java.time.LocalDateTime;
public record DischargeReportResponse(Integer id,Integer processId,String status,String patientName,String patientDni,String patientHistoryNumber,String psychologistName,LocalDate treatmentStartDate,LocalDate treatmentEndDate,long treatmentDays,long sessionCount,String dischargeReason,String treatmentSummary,String achievements,String recommendations,String registeredBy,LocalDateTime dischargedAt){}
