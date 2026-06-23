package com.clinica.psicologia.service;

import com.clinica.psicologia.dto.PacienteRequestDTO;
import com.clinica.psicologia.dto.PacienteResponseDTO;
import com.clinica.psicologia.entity.Paciente;
import com.clinica.psicologia.repository.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private PacienteService service;

    @Test
    void crearPacienteGeneraHistoriaClinicaYNormalizaCampos() {
        PacienteRequestDTO request = pacienteValido();
        request.setNombres(" Ana Maria ");
        request.setTelefono("   ");
        request.setEmail(" ana@cpp.test ");

        when(pacienteRepository.existsByDni("12345678")).thenReturn(false);
        when(pacienteRepository.getMaxNumeroHistoria()).thenReturn(41);
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> {
            Paciente paciente = invocation.getArgument(0);
            paciente.setId(7);
            return paciente;
        });

        PacienteResponseDTO response = service.crearPaciente(request);

        assertThat(response.getId()).isEqualTo(7);
        assertThat(response.getNumeroHistoria()).isEqualTo("HC-0042");
        assertThat(response.getDni()).isEqualTo("12345678");
        assertThat(response.getNombres()).isEqualTo("Ana Maria");
        assertThat(response.getTelefono()).isNull();
        assertThat(response.getEmail()).isEqualTo("ana@cpp.test");
        assertThat(response.getActivo()).isTrue();

        ArgumentCaptor<Paciente> captor = ArgumentCaptor.forClass(Paciente.class);
        verify(pacienteRepository).save(captor.capture());
        assertThat(captor.getValue().getNumeroHistoria()).isEqualTo("HC-0042");
    }

    @Test
    void crearPacienteRechazaDniDuplicado() {
        PacienteRequestDTO request = pacienteValido();
        when(pacienteRepository.existsByDni("12345678")).thenReturn(true);

        assertThatThrownBy(() -> service.crearPaciente(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DNI");

        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    private PacienteRequestDTO pacienteValido() {
        PacienteRequestDTO request = new PacienteRequestDTO();
        request.setDni("12345678");
        request.setNombres("Ana");
        request.setApellidos("Torres");
        request.setFechaNacimiento(LocalDate.of(1995, 4, 12));
        request.setSexo("F");
        request.setDireccion("Av. Sur 123");
        return request;
    }
}
