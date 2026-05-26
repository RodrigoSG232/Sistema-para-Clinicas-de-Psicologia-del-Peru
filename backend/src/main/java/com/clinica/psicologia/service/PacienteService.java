package com.clinica.psicologia.service;

import com.clinica.psicologia.dto.PacienteRequestDTO;
import com.clinica.psicologia.dto.PacienteResponseDTO;
import com.clinica.psicologia.entity.Paciente;
import com.clinica.psicologia.repository.PacienteRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PacienteService {
    private final PacienteRepository pacienteRepository;

    public PacienteResponseDTO crearPaciente(PacienteRequestDTO request) {
        if (pacienteRepository.existsByDni(request.getDni())) {
            throw new IllegalStateException("Ya existe un paciente con ese DNI");
        }

        Integer maxNumeroHistoria = pacienteRepository.getMaxNumeroHistoria();
        String numeroHistoria = "HC-" + String.format("%04d", (maxNumeroHistoria == null ? 0 : maxNumeroHistoria) + 1);
        LocalDateTime fechaApertura = LocalDateTime.now();

        Paciente paciente = Paciente.builder()
                .numeroHistoria(numeroHistoria)
                .dni(request.getDni().trim())
                .nombres(request.getNombres().trim())
                .apellidos(request.getApellidos().trim())
                .fechaNacimiento(request.getFechaNacimiento())
                .sexo(request.getSexo().trim())
                .telefono(trimToNull(request.getTelefono()))
                .email(trimToNull(request.getEmail()))
                .direccion(trimToNull(request.getDireccion()))
                .fechaApertura(fechaApertura)
                .activo(true)
                .build();

        return toResponseDTO(pacienteRepository.save(paciente));
    }

    private PacienteResponseDTO toResponseDTO(Paciente paciente) {
        return new PacienteResponseDTO(
                paciente.getId(),
                paciente.getNumeroHistoria(),
                paciente.getDni(),
                paciente.getNombres(),
                paciente.getApellidos(),
                paciente.getFechaNacimiento(),
                paciente.getSexo(),
                paciente.getTelefono(),
                paciente.getEmail(),
                paciente.getDireccion(),
                paciente.getFechaApertura(),
                paciente.getActivo());
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
