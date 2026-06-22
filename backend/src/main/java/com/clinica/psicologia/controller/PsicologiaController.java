package com.clinica.psicologia.controller;

import com.clinica.psicologia.dto.CitaDTO;
import com.clinica.psicologia.dto.ProcesoTerapeuticoDTO;
import com.clinica.psicologia.dto.SesionDTO;
import com.clinica.psicologia.entity.*;
import com.clinica.psicologia.repository.*;
import com.clinica.psicologia.security.JwtUtil;
import com.clinica.psicologia.service.ProcesoTerapeuticoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/psicologia")
@RequiredArgsConstructor
public class PsicologiaController {

    // Transiciones de estado que este endpoint tiene permitido aplicar.
    // PAGADA->EN_PISO y EN_PISO/PAGADA->CANCELADA son responsabilidad de otros controllers.
    private static final Map<String, Set<String>> TRANSICIONES_VALIDAS = Map.of(
            "EN_PISO", Set.of("EN_CONSULTA"),
            "EN_CONSULTA", Set.of("ATENDIDA")
    );

    private final CitaRepository citaRepo;
    private final PsicologoRepository psicologoRepo;
    private final PacienteRepository pacienteRepo;
    private final ProcesoTerapeuticoRepository procesoRepo;
    private final SesionRepository sesionRepo;
    private final UsuarioRepository usuarioRepo;
    private final JwtUtil jwtUtil;
    private final ProcesoTerapeuticoService procesoService;

    // ─── AGENDA ────────────────────────────────────────────────────────────────

    @GetMapping("/agenda")
    public ResponseEntity<?> getAgenda(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String fecha) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        Usuario usuario = usuarioRepo.findByUsername(username).orElseThrow();

        return psicologoRepo.findByUsuarioId(usuario.getId()).map(psic -> {
            LocalDate fechaDate = fecha != null ? LocalDate.parse(fecha) : LocalDate.now();
            List<Cita> citas = citaRepo.findByPsicologoFecha(psic.getId(), fechaDate);

            // ✅ Mapear a DTO en vez de retornar la entidad directamente
            List<CitaDTO> citasDTO = citas.stream().map(c -> new CitaDTO(
                c.getId(),
                c.getFechaCita().toString(),
                c.getHoraCita().toString(),
                c.getEstado(),
                psic.getNombres() + " " + psic.getApellidos(),
                c.getEspecialidad().getNombre(),
                c.getPaciente().getNombres() + " " + c.getPaciente().getApellidos(),
                c.getPaciente().getId(),           // ← pacienteId
                c.getPaciente().getDni(),          // ← pacienteDni
                c.getPaciente().getNumeroHistoria() // ← pacienteHc (verifica el nombre del campo)
            )).toList();

            return ResponseEntity.ok(Map.of(
                "psicologoId", psic.getId(),
                "nombreCompleto", psic.getNombres() + " " + psic.getApellidos(),
                "citas", citasDTO,
                "fecha", fechaDate
            ));
    }).orElse(ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "No es un psicólogo registrado")));
}

    @PatchMapping("/citas/{id}/estado")
    public ResponseEntity<?> cambiarEstadoCita(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }
        String nuevoEstado = body.get("estado");
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El campo 'estado' es requerido"));
        }
        return citaRepo.findById(id).map(c -> {
            Set<String> permitidos = TRANSICIONES_VALIDAS.getOrDefault(c.getEstado(), Set.of());
            if (!permitidos.contains(nuevoEstado)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No se puede pasar de " + c.getEstado() + " a " + nuevoEstado));
            }
            c.setEstado(nuevoEstado);
            return ResponseEntity.ok(toCitaDTO(citaRepo.save(c)));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── HISTORIA CLÍNICA / PROCESO TERAPÉUTICO ───────────────────────────────

    @GetMapping("/pacientes/{pacienteId}/proceso")
    public ResponseEntity<ProcesoTerapeuticoDTO> getProceso(@PathVariable Integer pacienteId) {
        return ResponseEntity.ok(procesoService.obtenerProcesoActivo(pacienteId));
    }

    @PostMapping("/pacientes/{pacienteId}/proceso")
    public ResponseEntity<?> iniciarProceso(
            @PathVariable Integer pacienteId,
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authHeader) {
        @SuppressWarnings("unchecked")
        Map<String, Object> entrevistaBody = (Map<String, Object>) body.get("entrevista");
        if (entrevistaBody == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "El campo 'entrevista' es requerido"));
        }
        String motivoConsulta = (String) entrevistaBody.get("motivoConsulta");
        if (motivoConsulta == null || motivoConsulta.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El motivo de consulta es requerido"));
        }
        Integer citaId = (Integer) body.get("citaId");
        if (citaId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "El campo 'citaId' es requerido"));
        }

        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        Usuario usuario = usuarioRepo.findByUsername(username).orElseThrow();
        Psicologo psic = psicologoRepo.findByUsuarioId(usuario.getId()).orElseThrow();
        Paciente  pac  = pacienteRepo.findById(pacienteId).orElseThrow();

        EntrevistaInicial entrevista = EntrevistaInicial.builder()
                .motivoConsulta(motivoConsulta)
                .antecedentesPersonales((String) entrevistaBody.get("antecedentesPersonales"))
                .antecedentesFamiliares((String) entrevistaBody.get("antecedentesFamiliares"))
                .observacionesIniciales((String) entrevistaBody.get("observacionesIniciales"))
                .build();

        ProcesoTerapeuticoDTO dto = procesoService.iniciarProcesoConEntrevista(pac, psic, citaId, usuario, entrevista);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PatchMapping("/procesos/{procesoId}/fase")
    public ResponseEntity<ProcesoTerapeuticoDTO> actualizarFase(
            @PathVariable Integer procesoId,
            @RequestBody Map<String, Object> body) {
        Integer nuevaFase = parseEntero(body.get("faseActual"));
        String observaciones = (String) body.get("observaciones");
        return ResponseEntity.ok(procesoService.actualizarFase(procesoId, nuevaFase, observaciones));
    }

    // ─── SESIONES ──────────────────────────────────────────────────────────────

    @PostMapping("/sesiones")
    public ResponseEntity<?> registrarSesion(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Integer citaId     = parseEntero(body.get("citaId"));
            Integer procesoId  = parseEntero(body.get("procesoId"));
            String  evolucion  = (String)  body.get("evolucion");
            String  indicaciones = (String) body.get("indicaciones");
            Integer faseSesion = parseEntero(body.get("faseSesion"));

            String token = authHeader.replace("Bearer ", "");
            String username = jwtUtil.extractUsername(token);
            Usuario usuario = usuarioRepo.findByUsername(username).orElse(null);

            Cita cita = citaRepo.findById(citaId).orElseThrow();
            ProcesoTerapeutico proceso = procesoRepo.findById(procesoId).orElseThrow();

            Sesion sesion = Sesion.builder()
                    .cita(cita)
                    .procesoTerapeutico(proceso)
                    .faseSesion(faseSesion != null ? faseSesion : proceso.getFaseActual())
                    .evolucion(evolucion)
                    .indicacionesPaciente(indicaciones)
                    .registradoPor(usuario)
                    .build();
            Sesion saved = sesionRepo.save(sesion);

            // Marcar cita como atendida
            cita.setEstado("ATENDIDA");
            citaRepo.save(cita);

            return ResponseEntity.status(HttpStatus.CREATED).body(toSesionDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sesiones/proceso/{procesoId}")
    public ResponseEntity<List<SesionDTO>> sesionesPorProceso(@PathVariable Integer procesoId) {
        return ResponseEntity.ok(
                sesionRepo.findByProcesoTerapeuticoIdOrderByFechaRegistroDesc(procesoId).stream()
                        .map(this::toSesionDTO)
                        .toList());
    }

    @GetMapping("/pacientes/{pacienteId}")
    public ResponseEntity<Paciente> getPaciente(@PathVariable Integer pacienteId) {
        return pacienteRepo.findById(pacienteId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Jackson deserializa los numeros de un Map<String,Object> como Integer,
    // pero si el cliente manda ese mismo campo como string (ej. el value de
    // un <select> nativo), llega como String. Acepta ambos en vez de romper
    // con ClassCastException.
    private Integer parseEntero(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Integer) return (Integer) valor;
        return Integer.parseInt(valor.toString());
    }

    private SesionDTO toSesionDTO(Sesion sesion) {
        return new SesionDTO(
                sesion.getId(),
                sesion.getFechaRegistro().toString(),
                sesion.getFaseSesion(),
                sesion.getEvolucion(),
                sesion.getIndicacionesPaciente(),
                sesion.getRegistradoPor() != null ? sesion.getRegistradoPor().getNombreCompleto() : null
        );
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
}
