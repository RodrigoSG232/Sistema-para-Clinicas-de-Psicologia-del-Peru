package com.clinica.psicologia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CitaResponseDTO {
    private Integer id;
    private String fecha;
    private String hora;
    private String psicologo;
    private String especialidad;
    private Double monto;
}