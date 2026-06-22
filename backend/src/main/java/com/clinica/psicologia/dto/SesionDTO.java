package com.clinica.psicologia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SesionDTO {
    private Integer id;
    private String fechaRegistro;
    private Integer faseSesion;
    private String evolucion;
    private String indicacionesPaciente;
    private String registradoPor;
}
