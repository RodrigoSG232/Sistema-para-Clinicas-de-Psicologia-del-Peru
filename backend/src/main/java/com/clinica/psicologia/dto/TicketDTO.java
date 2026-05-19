package com.clinica.psicologia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TicketDTO {

    private Integer id;
    private String numero;
    private String fechaEmision;
    private String estado;
}