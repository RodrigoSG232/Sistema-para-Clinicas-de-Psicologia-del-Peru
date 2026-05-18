package com.clinica.psicologia.dto;

import lombok.Data;

@Data
public class CitaRequestDTO {
    private Integer pacienteId;
    private Integer psicologoId;
    private Integer especialidadId;
    private Integer ticketId;
    private String fecha; // formato: yyyy-MM-dd
    private String hora;  // formato: HH:mm
}