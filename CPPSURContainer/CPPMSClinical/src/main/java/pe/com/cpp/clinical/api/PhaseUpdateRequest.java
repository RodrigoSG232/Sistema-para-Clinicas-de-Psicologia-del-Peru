package pe.com.cpp.clinical.api;
import jakarta.validation.constraints.Max; import jakarta.validation.constraints.Min; import jakarta.validation.constraints.NotNull;
public record PhaseUpdateRequest(@NotNull @Min(1) @Max(4) Integer faseActual,String observaciones){}
