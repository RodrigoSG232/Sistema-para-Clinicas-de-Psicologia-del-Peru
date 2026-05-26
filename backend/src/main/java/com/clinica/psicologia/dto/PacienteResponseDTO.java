package com.clinica.psicologia.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PacienteResponseDTO {
    private Integer id;
    private String numeroHistoria;
    private String dni;
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String sexo;
    private String telefono;
    private String email;
    private String direccion;
    private LocalDateTime fechaApertura;
    private Boolean activo;
}
