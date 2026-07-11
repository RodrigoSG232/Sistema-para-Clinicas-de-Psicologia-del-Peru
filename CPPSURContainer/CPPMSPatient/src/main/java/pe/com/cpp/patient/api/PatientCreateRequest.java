package pe.com.cpp.patient.api;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PatientCreateRequest(
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos numéricos")
        String dni,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no deben superar los 100 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no deben superar los 100 caracteres")
        String apellidos,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a la fecha actual")
        LocalDate fechaNacimiento,

        @NotBlank(message = "El sexo es obligatorio")
        @Pattern(regexp = "M|F", message = "El sexo debe ser M o F")
        String sexo,

        @Pattern(regexp = "^$|\\d{9}", message = "El teléfono debe tener 9 dígitos numéricos")
        String telefono,

        @Email(message = "El email no tiene un formato válido")
        @Size(max = 150, message = "El email no debe superar los 150 caracteres")
        String email,

        @Size(max = 250, message = "La dirección no debe superar los 250 caracteres")
        String direccion) {
}
