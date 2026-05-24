package com.clinica.psicologia.controller;

import com.clinica.psicologia.entity.*;
import com.clinica.psicologia.repository.*;
import com.clinica.psicologia.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.clinica.psicologia.dto.DeudaDTO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/caja")
@RequiredArgsConstructor
public class CajaController {

    private final DeudaRepository deudaRepo;
    private final ComprobantePagoRepository comprobanteRepo;
    private final PacienteRepository pacienteRepo;
    private final CitaRepository citaRepo;
    private final UsuarioRepository usuarioRepo;
    private final JwtUtil jwtUtil;

    // ─── BÚSQUEDA DE DEUDAS ────────────────────────────────────────────────────

    @GetMapping("/deudas/buscar")
    public ResponseEntity<List<DeudaDTO>> buscarDeudas(
            @RequestParam(required = false) String paciente,
            @RequestParam(required = false) String concepto) {

        List<Deuda> resultado;

        if (paciente != null && !paciente.isBlank()) {
            resultado = deudaRepo.buscarPorPaciente(paciente);
        } else {
            resultado = deudaRepo.findByEstado("PENDIENTE");
        }

        if (concepto != null && !concepto.isBlank() && !concepto.equals("Todos los conceptos")) {
            resultado = resultado.stream()
                    .filter(d -> d.getConcepto().toLowerCase().contains(concepto.toLowerCase()))
                    .toList();
        }

        List<DeudaDTO> response = resultado.stream()
        .map(d -> new DeudaDTO(
            d.getId(),
            d.getPaciente().getNombres() + " " + d.getPaciente().getApellidos(),
            d.getPaciente().getDni(),
            d.getConcepto(),
            d.getCita() != null ? d.getCita().getEspecialidad().getNombre() : null,
            d.getMonto().doubleValue(),
            d.getEstado()
        ))
        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/deudas/paciente/{pacienteId}")
    public ResponseEntity<List<DeudaDTO>> deudasPorPaciente(
            @PathVariable Integer pacienteId,
            @RequestParam(required = false) String concepto) {

        List<Deuda> resultado = deudaRepo.buscarPendientes(pacienteId, concepto);

        List<DeudaDTO> response = resultado.stream()
        .map(d -> new DeudaDTO(
            d.getId(),
            d.getPaciente().getNombres() + " " + d.getPaciente().getApellidos(),
            d.getPaciente().getDni(),
            d.getConcepto(),
            d.getCita() != null ? d.getCita().getEspecialidad().getNombre() : null,
            d.getMonto().doubleValue(),
            d.getEstado()
        ))
        .toList();

        return ResponseEntity.ok(response);
    }
    // ─── PROCESAR PAGO ─────────────────────────────────────────────────────────

    @PostMapping("/pagar/{deudaId}")
    public ResponseEntity<?> procesarPago(
            @PathVariable Integer deudaId,
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authHeader) {
        return deudaRepo.findById(deudaId).map(deuda -> {
            if ("PAGADO".equals(deuda.getEstado())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Esta deuda ya fue pagada"));
            }

            String medioPago = (String) body.get("medioPago");
            String tipo      = body.containsKey("tipo") ? (String) body.get("tipo") : "BOLETA";

            // Obtener cajero desde JWT
            String token    = authHeader.replace("Bearer ", "");
            String username = jwtUtil.extractUsername(token);
            Usuario cajero  = usuarioRepo.findByUsername(username).orElse(null);

            // Número de comprobante correlativo
            int correlativo = comprobanteRepo.getMaxCorrelativo() + 1;
            String numComp  = "B-" + String.format("%05d", correlativo);

            ComprobantePago comp = ComprobantePago.builder()
                    .numeroComprobante(numComp)
                    .tipo(tipo)
                    .deuda(deuda)
                    .medioPago(medioPago)
                    .montoPagado(deuda.getMonto())
                    .cajero(cajero)
                    .build();
            comprobanteRepo.save(comp);

            // Marcar deuda como pagada
            deuda.setEstado("PAGADA");
            deudaRepo.save(deuda);

            // Marcar cita como PAGADA
            if (deuda.getCita() != null) {
                Cita cita = deuda.getCita();
                cita.setEstado("PAGADA");
                citaRepo.save(cita);
            }

            return ResponseEntity.ok(Map.of(
                    "comprobante", comp,
                    "numeroComprobante", numComp,
                    "mensaje", "Pago registrado exitosamente"
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── COMPROBANTES ──────────────────────────────────────────────────────────

    @GetMapping("/comprobantes/{id}")
    public ResponseEntity<ComprobantePago> getComprobante(@PathVariable Integer id) {
        return comprobanteRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
