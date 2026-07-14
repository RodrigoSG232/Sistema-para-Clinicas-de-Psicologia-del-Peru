package pe.com.cpp.clinical.repository;
import java.time.LocalDateTime;
public interface ProductivityCaseProjection {Integer getReportId();String getTicketNumber();String getPatientName();String getPatientHistoryNumber();String getPsychologistName();LocalDateTime getTicketIssuedAt();LocalDateTime getDischargedAt();Double getEfficiencyHours();}
