package com.clinica.psicologia.controller;

import com.clinica.psicologia.dto.CitaDTO;
import com.clinica.psicologia.dto.TurnoNotificacionDTO;
import com.clinica.psicologia.entity.Cita;
import com.clinica.psicologia.repository.CitaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enfermera")
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "legacy.business-api.enabled", havingValue = "true")
public class EnfermeraController {

    private static final String ESTADO_PAGADA = "PAGADA";
    private static final String ESTADO_EN_PISO = "EN_PISO";

    private final CitaRepository citaRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/pacientes-piso")
    public ResponseEntity<List<CitaDTO>> pacientesEnPiso(@RequestParam(required = false) String fecha) {
        LocalDate fechaConsulta = fecha != null && !fecha.isBlank()
                ? LocalDate.parse(fecha)
                : LocalDate.now();

        List<CitaDTO> citas = citaRepo.findByFechaAndEstadoIn(fechaConsulta, List.of(ESTADO_PAGADA, ESTADO_EN_PISO))
                .stream()
                .map(this::toCitaDTO)
                .toList();

        return ResponseEntity.ok(citas);
    }

    @PatchMapping("/citas/{id}/en-piso")
    public ResponseEntity<?> confirmarEnPiso(@PathVariable Integer id) {
        return citaRepo.findById(id).map(cita -> {
            if (!ESTADO_PAGADA.equals(cita.getEstado())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Solo se puede confirmar una cita pagada"));
            }

            cita.setEstado(ESTADO_EN_PISO);
            Cita guardada = citaRepo.save(cita);
            notificarPacienteListo(guardada);

            return ResponseEntity.ok(toCitaDTO(guardada));
        }).orElse(ResponseEntity.notFound().build());
    }

    private CitaDTO toCitaDTO(Cita cita) {
        return new CitaDTO(
                cita.getId(),
                cita.getFechaCita().toString(),
                cita.getHoraCita().toString(),
                cita.getEstado(),
                cita.getPsicologo().getNombres() + " " + cita.getPsicologo().getApellidos(),
                cita.getEspecialidad().getNombre(),
                cita.getPaciente().getNombres() + " " + cita.getPaciente().getApellidos(),
                cita.getPaciente().getId(),
                cita.getPaciente().getDni(),
                cita.getPaciente().getNumeroHistoria()
        );
    }

    private void notificarPacienteListo(Cita cita) {
        TurnoNotificacionDTO notificacion = new TurnoNotificacionDTO(
                cita.getId(),
                cita.getPaciente().getId(),
                cita.getPaciente().getNombres() + " " + cita.getPaciente().getApellidos(),
                cita.getPaciente().getDni(),
                cita.getPaciente().getNumeroHistoria(),
                cita.getPsicologo().getId(),
                cita.getPsicologo().getNombres() + " " + cita.getPsicologo().getApellidos(),
                cita.getEspecialidad().getNombre(),
                cita.getFechaCita().toString(),
                cita.getHoraCita().toString(),
                cita.getEstado(),
                cita.getTicket() != null ? cita.getTicket().getNumero() : null,
                "Paciente confirmado en piso"
        );

        messagingTemplate.convertAndSend("/topic/psicologos/" + cita.getPsicologo().getId() + "/pacientes-listos", notificacion);
    }
}
