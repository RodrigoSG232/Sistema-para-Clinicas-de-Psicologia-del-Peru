package com.clinica.psicologia.service;

import com.clinica.psicologia.dto.EntrevistaInicialDTO;
import com.clinica.psicologia.dto.ProcesoTerapeuticoDTO;
import com.clinica.psicologia.entity.Cita;
import com.clinica.psicologia.entity.EntrevistaInicial;
import com.clinica.psicologia.entity.Paciente;
import com.clinica.psicologia.entity.ProcesoTerapeutico;
import com.clinica.psicologia.entity.Psicologo;
import com.clinica.psicologia.entity.Sesion;
import com.clinica.psicologia.entity.Usuario;
import com.clinica.psicologia.repository.CitaRepository;
import com.clinica.psicologia.repository.EntrevistaInicialRepository;
import com.clinica.psicologia.repository.ProcesoTerapeuticoRepository;
import com.clinica.psicologia.repository.SesionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ProcesoTerapeuticoService {

    private final ProcesoTerapeuticoRepository procesoRepo;
    private final EntrevistaInicialRepository entrevistaRepo;
    private final SesionRepository sesionRepo;
    private final CitaRepository citaRepo;

    // Crea el proceso + la entrevista inicial y, en el mismo acto, registra la
    // Sesion de fase 1 y marca la Cita como ATENDIDA. Se hace todo junto porque
    // la primera visita de un paciente es a la vez la entrevista y la atención
    // real: separar esto en dos pasos dejaría la cita trabada en EN_CONSULTA
    // si el psicólogo no volviera a guardar una segunda vez.
    @Transactional
    public ProcesoTerapeuticoDTO iniciarProcesoConEntrevista(
            Paciente paciente, Psicologo psicologo, Integer citaId, Usuario registradoPor, EntrevistaInicial entrevista) {
        if (procesoRepo.findByPacienteIdAndActivoTrue(paciente.getId()).isPresent()) {
            throw new IllegalStateException("El paciente ya tiene un proceso terapéutico activo");
        }

        Cita cita = citaRepo.findById(citaId)
                .orElseThrow(() -> new NoSuchElementException("Cita no encontrada"));
        if (!"EN_CONSULTA".equals(cita.getEstado())) {
            throw new IllegalStateException("La cita debe estar en consulta para registrar la entrevista inicial");
        }

        ProcesoTerapeutico proceso = procesoRepo.save(
                ProcesoTerapeutico.builder()
                        .paciente(paciente)
                        .psicologo(psicologo)
                        .faseActual(1)
                        .build()
        );

        entrevista.setProcesoTerapeutico(proceso);
        EntrevistaInicial entrevistaGuardada = entrevistaRepo.save(entrevista);

        Sesion sesion = Sesion.builder()
                .cita(cita)
                .procesoTerapeutico(proceso)
                .faseSesion(1)
                .evolucion(formatearEvolucion(entrevistaGuardada))
                .registradoPor(registradoPor)
                .build();
        sesionRepo.save(sesion);

        cita.setEstado("ATENDIDA");
        citaRepo.save(cita);

        return toDto(proceso, entrevistaGuardada);
    }

    @Transactional(readOnly = true)
    public ProcesoTerapeuticoDTO obtenerProcesoActivo(Integer pacienteId) {
        ProcesoTerapeutico proceso = procesoRepo.findByPacienteIdAndActivoTrue(pacienteId)
                .orElseThrow(() -> new NoSuchElementException("El paciente no tiene un proceso terapéutico activo"));

        EntrevistaInicial entrevista = entrevistaRepo.findByProcesoTerapeuticoId(proceso.getId()).orElse(null);
        return toDto(proceso, entrevista);
    }

    @Transactional
    public ProcesoTerapeuticoDTO actualizarFase(Integer procesoId, Integer nuevaFase, String observaciones) {
        if (nuevaFase == null || nuevaFase < 1 || nuevaFase > 4) {
            throw new IllegalArgumentException("Fase inválida (1-4)");
        }

        ProcesoTerapeutico proceso = procesoRepo.findById(procesoId)
                .orElseThrow(() -> new NoSuchElementException("Proceso terapéutico no encontrado"));
        if (!Boolean.TRUE.equals(proceso.getActivo())) {
            throw new IllegalStateException("La historia clínica está cerrada por alta");
        }

        proceso.setFaseActual(nuevaFase);
        if (observaciones != null) {
            proceso.setObservaciones(observaciones);
        }
        ProcesoTerapeutico guardado = procesoRepo.save(proceso);

        EntrevistaInicial entrevista = entrevistaRepo.findByProcesoTerapeuticoId(guardado.getId()).orElse(null);
        return toDto(guardado, entrevista);
    }

    @Transactional
    public ProcesoTerapeuticoDTO cerrarPorAlta(Integer procesoId) {
        ProcesoTerapeutico proceso = procesoRepo.findById(procesoId)
                .orElseThrow(() -> new NoSuchElementException("Proceso terapéutico no encontrado"));
        if (Boolean.TRUE.equals(proceso.getActivo())) {
            proceso.setActivo(false);
            proceso.setFechaFin(java.time.LocalDate.now());
            proceso = procesoRepo.save(proceso);
        }
        EntrevistaInicial entrevista = entrevistaRepo.findByProcesoTerapeuticoId(proceso.getId()).orElse(null);
        return toDto(proceso, entrevista);
    }

    private ProcesoTerapeuticoDTO toDto(ProcesoTerapeutico p, EntrevistaInicial entrevista) {
        EntrevistaInicialDTO entrevistaDTO = entrevista == null ? null : new EntrevistaInicialDTO(
                entrevista.getId(),
                entrevista.getMotivoConsulta(),
                entrevista.getAntecedentesPersonales(),
                entrevista.getAntecedentesFamiliares(),
                entrevista.getObservacionesIniciales(),
                entrevista.getFechaRegistro().toString()
        );

        return new ProcesoTerapeuticoDTO(
                p.getId(),
                p.getPaciente().getId(),
                p.getPsicologo().getId(),
                p.getFaseActual(),
                p.getFechaInicio().toString(),
                p.getFechaFin() != null ? p.getFechaFin().toString() : null,
                p.getObservaciones(),
                p.getActivo(),
                entrevistaDTO
        );
    }

    private String formatearEvolucion(EntrevistaInicial entrevista) {
        StringBuilder sb = new StringBuilder("Motivo de consulta: ").append(entrevista.getMotivoConsulta());
        if (entrevista.getAntecedentesPersonales() != null && !entrevista.getAntecedentesPersonales().isBlank()) {
            sb.append("\n\nObservaciones clínicas: ").append(entrevista.getAntecedentesPersonales());
        }
        if (entrevista.getObservacionesIniciales() != null && !entrevista.getObservacionesIniciales().isBlank()) {
            sb.append("\n\nResultado / hipótesis: ").append(entrevista.getObservacionesIniciales());
        }
        return sb.toString();
    }
}
