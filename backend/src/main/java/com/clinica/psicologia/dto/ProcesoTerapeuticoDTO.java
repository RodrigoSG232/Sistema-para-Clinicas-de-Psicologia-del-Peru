package com.clinica.psicologia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProcesoTerapeuticoDTO {
    private Integer id;
    private Integer pacienteId;
    private Integer psicologoId;
    private Integer faseActual;
    private String fechaInicio;
    private String fechaFin;
    private String observaciones;
    private Boolean activo;
    private EntrevistaInicialDTO entrevistaInicial;
}
