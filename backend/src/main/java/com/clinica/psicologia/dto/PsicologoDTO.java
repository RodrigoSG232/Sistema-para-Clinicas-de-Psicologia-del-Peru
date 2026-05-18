package com.clinica.psicologia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PsicologoDTO {
    private Integer id;
    private String nombres;
    private String apellidos;
    private String especialidad;
}