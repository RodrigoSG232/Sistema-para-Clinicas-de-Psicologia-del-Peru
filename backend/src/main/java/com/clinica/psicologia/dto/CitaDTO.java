package com.clinica.psicologia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CitaDTO {
    private Integer id;
    private String fecha;
    private String hora;
    private String estado;
    private String psicologo;
    private String especialidad;
    private String paciente;
    private Integer pacienteId;
    private String pacienteDni;
    private String pacienteHc;
}