package pe.com.cpp.clinical.api;
import jakarta.validation.constraints.NotBlank;
public record InitialInterviewRequest(@NotBlank(message="El motivo de consulta es obligatorio") String motivoConsulta,String antecedentesPersonales,String antecedentesFamiliares,String observacionesIniciales){}
