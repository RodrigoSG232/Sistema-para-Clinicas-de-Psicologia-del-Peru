package com.clinica.psicologia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EntrevistaInicialDTO {
    private Integer id;
    private String motivoConsulta;
    private String antecedentesPersonales;
    private String antecedentesFamiliares;
    private String observacionesIniciales;
    private String fechaRegistro;
}
