package pe.com.cpp.clinical.api;
import java.time.LocalDate; import java.util.List;
public record ProductivityReportResponse(LocalDate fromDate,LocalDate toDate,long totalDischarges,double averageHours,double minimumHours,double maximumHours,List<ProductivityTrendResponse> dailyTrend,List<PsychologistProductivityResponse> byPsychologist,List<ProductivityCaseResponse> cases){}
