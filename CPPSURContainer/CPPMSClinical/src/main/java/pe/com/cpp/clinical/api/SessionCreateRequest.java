package pe.com.cpp.clinical.api;
import jakarta.validation.constraints.*;
public record SessionCreateRequest(@NotNull Integer processId,@NotNull Integer appointmentId,@Min(1) @Max(4) Integer sessionPhase,@NotBlank String evolution,String patientIndications,@NotBlank String registeredBy){}
