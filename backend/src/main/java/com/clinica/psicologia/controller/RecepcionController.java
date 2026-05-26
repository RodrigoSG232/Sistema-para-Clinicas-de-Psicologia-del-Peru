package com.clinica.psicologia.controller;

import com.clinica.psicologia.entity.*;
import com.clinica.psicologia.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.clinica.psicologia.dto.CitaDTO;
import com.clinica.psicologia.dto.CitaRequestDTO;
import com.clinica.psicologia.dto.CitaResponseDTO;
import com.clinica.psicologia.dto.PsicologoDTO;
import com.clinica.psicologia.dto.TicketDTO;
import com.clinica.psicologia.service.TicketService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recepcion")
@RequiredArgsConstructor
public class RecepcionController {

        private final TicketService ticketService;
        private final TicketRepository ticketRepo;
        private final PacienteRepository pacienteRepo;
        private final EspecialidadRepository especialidadRepo;
        private final PsicologoRepository psicologoRepo;
        private final CitaRepository citaRepo;
        private final DeudaRepository deudaRepo;
        private final UsuarioRepository usuarioRepo;

        // ─── TICKETS ──────────────────────────────────────────────────────────────

        @GetMapping("/tickets")
        public ResponseEntity<List<TicketDTO>> listarTickets(
                        @RequestParam(defaultValue = "ESPERA") String estado) {

                return ResponseEntity.ok(ticketService.listarPorEstado(estado));
        }

        @PostMapping("/tickets/emitir")
        public ResponseEntity<TicketDTO> emitirTicket() {
                return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.emitirTicket());
        }

        @GetMapping("/tickets/actual")
        public ResponseEntity<TicketDTO> obtenerTicketActual() {
                TicketDTO ticket = ticketService.obtenerTicketActual();
                return ticket != null ? ResponseEntity.ok(ticket) : ResponseEntity.noContent().build();
        }

        @PatchMapping("/tickets/{id}/llamar")
        public ResponseEntity<?> llamarTicket(@PathVariable Integer id) {
                try {
                        return ResponseEntity.ok(ticketService.llamarTicket(id));
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
                } catch (IllegalStateException e) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
                }
        }

        @PatchMapping("/tickets/{id}/finalizar")
        public ResponseEntity<?> finalizarTicket(@PathVariable Integer id) {
                try {
                        return ResponseEntity.ok(ticketService.finalizarTicket(id));
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
                } catch (IllegalStateException e) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
                }
        }

        @PatchMapping("/tickets/{id}/estado")
        public ResponseEntity<?> cambiarEstadoTicket(
                        @PathVariable Integer id,
                        @RequestBody Map<String, String> body) {

                String estado = body.get("estado");

                if ("EN_ATENCION".equals(estado)) {
                        return llamarTicket(id);
                }

                if ("FINALIZADO".equals(estado)) {
                        return finalizarTicket(id);
                }

                return ResponseEntity.badRequest()
                                .body(Map.of("error", "Estado no soportado para este endpoint"));
        }

        // ─── PACIENTES / HISTORIA CLÍNICA ─────────────────────────────────────────

        @GetMapping("/pacientes/buscar")
        public ResponseEntity<List<Paciente>> buscarPacientes(@RequestParam String q) {
                return ResponseEntity.ok(pacienteRepo.buscar(q));
        }

        @GetMapping("/pacientes/{id}")
        public ResponseEntity<Paciente> getPaciente(@PathVariable Integer id) {
                return pacienteRepo.findById(id)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        @GetMapping("/pacientes/dni/{dni}")
        public ResponseEntity<Paciente> getPacientePorDni(@PathVariable String dni) {
                return pacienteRepo.findByDni(dni)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        @PostMapping("/pacientes")
        public ResponseEntity<?> crearPaciente(@RequestBody Paciente p) {
                if (pacienteRepo.existsByDni(p.getDni())) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(Map.of("error", "Ya existe un paciente con ese DNI"));
                }
                int maxNum = pacienteRepo.getMaxNumeroHistoria();
                p.setNumeroHistoria("HC-" + String.format("%04d", maxNum + 1));
                p.setFechaApertura(LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.CREATED).body(pacienteRepo.save(p));
        }

        // ─── ESPECIALIDADES Y PSICÓLOGOS ──────────────────────────────────────────

        @GetMapping("/especialidades")
        public ResponseEntity<List<Especialidad>> listarEspecialidades() {
                return ResponseEntity.ok(especialidadRepo.findByActivoTrue());
        }

        @GetMapping("/psicologos")
        public ResponseEntity<List<PsicologoDTO>> listarPsicologos(
                        @RequestParam(required = false) Integer especialidadId) {

                List<Psicologo> lista = especialidadId != null
                                ? psicologoRepo.findByEspecialidadId(especialidadId)
                                : psicologoRepo.findByActivoTrue();

                List<PsicologoDTO> resultado = lista.stream()
                                .map(p -> new PsicologoDTO(
                                                p.getId(),
                                                p.getNombres(),
                                                p.getApellidos(),
                                                p.getEspecialidad() != null ? p.getEspecialidad().getNombre()
                                                                : "Sin especialidad"))
                                .toList();

                return ResponseEntity.ok(resultado);
        }

        @GetMapping("/psicologos/{id}/horario-disponible")
        public ResponseEntity<Map<String, Object>> horarioDisponible(
                        @PathVariable Integer id,
                        @RequestParam String fecha) {
                LocalDate fechaDate = LocalDate.parse(fecha);
                List<Cita> citasOcupadas = citaRepo.findHorasOcupadas(id, fechaDate);
                List<String> horasOcupadas = citasOcupadas.stream()
                                .map(c -> c.getHoraCita().format(DateTimeFormatter.ofPattern("HH:mm")))
                                .toList();
                // Franjas: 08:00-19:00 cada 1 hora
                List<String> todasHoras = List.of(
                                "08:00", "09:00", "10:00", "11:00", "12:00",
                                "13:00", "14:00", "15:00", "16:00", "17:00", "18:00");
                List<String> disponibles = todasHoras.stream()
                                .filter(h -> !horasOcupadas.contains(h))
                                .toList();
                return ResponseEntity.ok(Map.of("horasDisponibles", disponibles, "horasOcupadas", horasOcupadas));
        }

        // ─── CITAS ────────────────────────────────────────────────────────────────

        @PostMapping("/citas")
        public ResponseEntity<?> registrarCita(@RequestBody CitaRequestDTO body) {
                try {
                        Integer pacienteId = body.getPacienteId();
                        Integer psicologoId = body.getPsicologoId();
                        Integer especialidadId = body.getEspecialidadId();
                        Integer ticketId = body.getTicketId();
                        String fechaStr = body.getFecha();
                        String horaStr = body.getHora();

                        // ─── Validaciones básicas ───────────────────────────
                        if (pacienteId == null || psicologoId == null || especialidadId == null
                                        || fechaStr == null || horaStr == null) {
                                return ResponseEntity.badRequest()
                                                .body(Map.of("error", "Faltan campos obligatorios"));
                        }

                        LocalDate fecha = LocalDate.parse(fechaStr);
                        LocalTime hora = LocalTime.parse(horaStr);

                        // ❌ Validar domingo (no permitido)
                        if (fecha.getDayOfWeek().getValue() == 7) {
                                return ResponseEntity.badRequest()
                                                .body(Map.of("error", "No se pueden agendar citas los domingos"));
                        }

                        // ❌ Validar horario (08:00 - 18:00)
                        if (hora.isBefore(LocalTime.of(8, 0)) || hora.isAfter(LocalTime.of(18, 0))) {
                                return ResponseEntity.badRequest()
                                                .body(Map.of("error",
                                                                "Hora fuera de horario permitido (08:00 - 18:00)"));
                        }

                        // ❌ Validar duplicado (misma fecha, hora y psicólogo)
                        boolean existe = citaRepo.existeCita(
                                        psicologoId, fecha, hora.toString());

                        if (existe) {
                                return ResponseEntity.badRequest()
                                                .body(Map.of("error", "Esa hora ya está reservada"));
                        }

                        // ─── Obtener entidades ──────────────────────────────
                        Paciente paciente = pacienteRepo.findById(pacienteId).orElseThrow();
                        Psicologo psicologo = psicologoRepo.findById(psicologoId).orElseThrow();
                        Especialidad especialidad = especialidadRepo.findById(especialidadId).orElseThrow();

                        // ─── Crear cita ─────────────────────────────────────
                        Cita cita = Cita.builder()
                                        .paciente(paciente)
                                        .psicologo(psicologo)
                                        .especialidad(especialidad)
                                        .fechaCita(fecha)
                                        .horaCita(hora)
                                        .estado("PENDIENTE_PAGO")
                                        .build();

                        // Ticket opcional
                        if (ticketId != null) {
                                ticketRepo.findById(ticketId).ifPresent(t -> {
                                        cita.setTicket(t);
                                        t.setEstado("EN_ATENCION");
                                        // CORREGIDO: Se eliminó t.setPaciente(paciente);
                                        ticketRepo.save(t);
                                });
                        }

                        Cita citaGuardada = citaRepo.save(cita);

                        // ─── Generar deuda ──────────────────────────────────
                        Deuda deuda = Deuda.builder()
                                        .paciente(paciente)
                                        .cita(citaGuardada)
                                        .concepto("Gastos de cita")
                                        .monto(especialidad.getTarifa())
                                        .estado("PENDIENTE")
                                        .build();

                        deudaRepo.save(deuda);

                        // ─── Response limpio (DTO) ──────────────────────────
                        CitaResponseDTO response = new CitaResponseDTO(
                                        citaGuardada.getId(),
                                        fecha.toString(),
                                        hora.toString(),
                                        psicologo.getNombres() + " " + psicologo.getApellidos(),
                                        especialidad.getNombre(),
                                        deuda.getMonto().doubleValue());

                        return ResponseEntity.status(HttpStatus.CREATED).body(response);

                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("error", "Error al registrar la cita: " + e.getMessage()));
                }
        }

        @GetMapping("/citas/paciente/{pacienteId}")
        public ResponseEntity<List<CitaDTO>> citasPorPaciente(@PathVariable Integer pacienteId) {
                List<Cita> citas = citaRepo.findByPacienteIdOrderByCreadoEnDesc(pacienteId);

                List<CitaDTO> citasDTO = citas.stream().map(c -> new CitaDTO(
                                c.getId(),
                                c.getFechaCita().toString(),
                                c.getHoraCita().toString(),
                                c.getEstado(),
                                c.getPsicologo().getNombres() + " " + c.getPsicologo().getApellidos(),
                                c.getEspecialidad().getNombre(),
                                c.getPaciente().getNombres() + " " + c.getPaciente().getApellidos(),
                                c.getPaciente().getId(),
                                c.getPaciente().getDni(),
                                c.getPaciente().getNumeroHistoria())).toList();

                return ResponseEntity.ok(citasDTO);
        }
}
