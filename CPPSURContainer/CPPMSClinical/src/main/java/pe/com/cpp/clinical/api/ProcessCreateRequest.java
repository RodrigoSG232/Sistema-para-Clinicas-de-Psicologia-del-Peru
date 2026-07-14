package pe.com.cpp.clinical.api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
public record ProcessCreateRequest(@NotNull Integer appointmentId,@NotNull Integer psychologistId,String observaciones,@NotNull @Valid InitialInterviewRequest entrevista){}
