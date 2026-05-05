package com.clinica.psicologia.controller;

import com.clinica.psicologia.entity.*;
import com.clinica.psicologia.repository.*;
import com.clinica.psicologia.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/psicologia")
@RequiredArgsConstructor
public class PsicologiaController {

    private final CitaRepository citaRepo;
    private final PsicologoRepository psicologoRepo;
    private final PacienteRepository pacienteRepo;
    private final ProcesoTerapeuticoRepository procesoRepo;
    private final SesionRepository sesionRepo;
    private final UsuarioRepository usuarioRepo;
    private final JwtUtil jwtUtil;

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
            return ResponseEntity.ok(Map.of("psicologo", psic, "citas", citas, "fecha", fechaDate));
        }).orElse(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "No es un psicólogo registrado")));
    }

    @PatchMapping("/citas/{id}/estado")
    public ResponseEntity<Cita> cambiarEstadoCita(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        return citaRepo.findById(id).map(c -> {
            c.setEstado(body.get("estado"));
            return ResponseEntity.ok(citaRepo.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── HISTORIA CLÍNICA / PROCESO TERAPÉUTICO ───────────────────────────────

    @GetMapping("/pacientes/{pacienteId}/proceso")
    public ResponseEntity<?> getProceso(@PathVariable Integer pacienteId) {
        return procesoRepo.findByPacienteIdAndActivoTrue(pacienteId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/pacientes/{pacienteId}/proceso")
    public ResponseEntity<?> iniciarProceso(
            @PathVariable Integer pacienteId,
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authHeader) {
        // Verificar que no tenga proceso activo
        if (procesoRepo.findByPacienteIdAndActivoTrue(pacienteId).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El paciente ya tiene un proceso terapéutico activo"));
        }

        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);
        Usuario usuario = usuarioRepo.findByUsername(username).orElseThrow();
        Psicologo psic = psicologoRepo.findByUsuarioId(usuario.getId()).orElseThrow();
        Paciente  pac  = pacienteRepo.findById(pacienteId).orElseThrow();

        ProcesoTerapeutico proceso = ProcesoTerapeutico.builder()
                .paciente(pac).psicologo(psic).faseActual(1).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(procesoRepo.save(proceso));
    }

    @PatchMapping("/procesos/{procesoId}/fase")
    public ResponseEntity<?> actualizarFase(
            @PathVariable Integer procesoId,
            @RequestBody Map<String, Object> body) {
        return procesoRepo.findById(procesoId).map(p -> {
            Integer nuevaFase = (Integer) body.get("faseActual");
            if (nuevaFase < 1 || nuevaFase > 4)
                return ResponseEntity.badRequest().body(Map.of("error", "Fase inválida (1-4)"));
            p.setFaseActual(nuevaFase);
            if (body.containsKey("observaciones"))
                p.setObservaciones((String) body.get("observaciones"));
            return ResponseEntity.ok(procesoRepo.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── SESIONES ──────────────────────────────────────────────────────────────

    @PostMapping("/sesiones")
    public ResponseEntity<?> registrarSesion(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Integer citaId     = (Integer) body.get("citaId");
            Integer procesoId  = (Integer) body.get("procesoId");
            String  evolucion  = (String)  body.get("evolucion");
            String  indicaciones = (String) body.get("indicaciones");
            Integer faseSesion = (Integer) body.get("faseSesion");

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

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sesiones/proceso/{procesoId}")
    public ResponseEntity<List<Sesion>> sesionesPorProceso(@PathVariable Integer procesoId) {
        return ResponseEntity.ok(
                sesionRepo.findByProcesoTerapeuticoIdOrderByFechaRegistroDesc(procesoId));
    }

    @GetMapping("/pacientes/{pacienteId}")
    public ResponseEntity<Paciente> getPaciente(@PathVariable Integer pacienteId) {
        return pacienteRepo.findById(pacienteId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
