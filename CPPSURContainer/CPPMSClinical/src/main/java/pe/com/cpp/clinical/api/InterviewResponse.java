package pe.com.cpp.clinical.api;
import java.time.LocalDateTime;
public record InterviewResponse(Integer id,String motivoConsulta,String antecedentesPersonales,String antecedentesFamiliares,String observacionesIniciales,LocalDateTime registradoEn){}
