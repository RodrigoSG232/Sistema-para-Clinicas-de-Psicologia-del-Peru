package pe.com.cpp.patient.api;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientResponse(
        Integer id,
        String numeroHistoria,
        String dni,
        String nombres,
        String apellidos,
        String nombreCompleto,
        LocalDate fechaNacimiento,
        String sexo,
        String telefono,
        String email,
        String direccion,
        LocalDateTime fechaApertura,
        Boolean activo) {
}
