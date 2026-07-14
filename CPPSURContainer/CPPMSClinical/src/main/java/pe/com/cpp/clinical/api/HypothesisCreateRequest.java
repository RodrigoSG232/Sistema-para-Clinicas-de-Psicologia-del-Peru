package pe.com.cpp.clinical.api;
import java.util.List; import jakarta.validation.constraints.*;
public record HypothesisCreateRequest(Integer sessionId,@NotBlank(message="La hipótesis clínica es obligatoria") String hypothesis,@NotBlank(message="El plan terapéutico es obligatorio") String therapeuticPlan,@NotEmpty(message="Debe seleccionar al menos un diagnóstico CIE-10") List<@NotBlank String> diagnosisCodes,@NotBlank(message="El profesional que registra es obligatorio") String registeredBy){}
