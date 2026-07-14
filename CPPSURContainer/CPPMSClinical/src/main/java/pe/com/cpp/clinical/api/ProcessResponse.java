package pe.com.cpp.clinical.api;
import java.time.LocalDate;
public record ProcessResponse(Integer id,Integer patientId,String patientName,String patientDni,String patientHistoryNumber,Integer psychologistId,String psychologistName,Integer currentPhase,LocalDate startDate,LocalDate endDate,String observations,boolean active,String status,InterviewResponse initialInterview){}
