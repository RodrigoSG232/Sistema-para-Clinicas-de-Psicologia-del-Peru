package pe.com.cpp.clinical.api;
import jakarta.validation.constraints.NotBlank;
public record DischargeRequest(@NotBlank(message="El motivo de alta es obligatorio") String dischargeReason,@NotBlank(message="El resumen del tratamiento es obligatorio") String treatmentSummary,@NotBlank(message="Los logros alcanzados son obligatorios") String achievements,@NotBlank(message="Las recomendaciones son obligatorias") String recommendations,@NotBlank(message="El profesional responsable es obligatorio") String registeredBy){}
