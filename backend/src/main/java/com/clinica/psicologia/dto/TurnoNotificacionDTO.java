package com.clinica.psicologia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TurnoNotificacionDTO {
    private Integer citaId;
    private Integer pacienteId;
    private String paciente;
    private String pacienteDni;
    private String pacienteHc;
    private Integer psicologoId;
    private String psicologo;
    private String especialidad;
    private String fecha;
    private String hora;
    private String estado;
    private String ticketNumero;
    private String mensaje;
}
