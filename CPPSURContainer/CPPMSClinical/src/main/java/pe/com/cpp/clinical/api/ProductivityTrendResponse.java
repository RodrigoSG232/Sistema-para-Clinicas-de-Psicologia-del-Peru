package pe.com.cpp.clinical.api;
import java.time.LocalDate;
public record ProductivityTrendResponse(LocalDate date,long discharges,double averageHours){}
