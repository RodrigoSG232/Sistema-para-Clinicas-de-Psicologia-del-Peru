
package com.clinica.psicologia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class DeudaDTO {

    private Integer id;

    private String pacienteNombre;
    private String pacienteDni;

    private String concepto;

    private String especialidad;

    private Double monto;

    private String estado;
}