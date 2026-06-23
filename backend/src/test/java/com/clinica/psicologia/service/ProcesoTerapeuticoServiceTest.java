package com.clinica.psicologia.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcesoTerapeuticoServiceTest {

    @Mock
    private ProcesoTerapeuticoRepository procesoRepository;

    @Mock
    private EntrevistaInicialRepository entrevistaRepository;

    @Mock
    private SesionRepository sesionRepository;

    @Mock
    private CitaRepository citaRepository;

    @InjectMocks
    private ProcesoTerapeuticoService service;

    @Test
    void iniciarProcesoConEntrevistaCreaProcesoSesionYMarcaCitaAtendida() {
        Paciente paciente = Paciente.builder().id(1).nombres("Ana").apellidos("Torres").build();
        Psicologo psicologo = Psicologo.builder().id(2).nombres("Luis").apellidos("Ramos").build();
        Usuario registradoPor = Usuario.builder().id(3).username("psicologo").build();
        Cita cita = Cita.builder().id(8).estado("EN_CONSULTA").build();
        EntrevistaInicial entrevista = EntrevistaInicial.builder()
                .motivoConsulta("Ansiedad")
                .antecedentesPersonales("Dificultad para dormir")
                .observacionesIniciales("Hipotesis inicial")
                .build();

        when(procesoRepository.findByPacienteIdAndActivoTrue(1)).thenReturn(Optional.empty());
        when(citaRepository.findById(8)).thenReturn(Optional.of(cita));
        when(procesoRepository.save(any(ProcesoTerapeutico.class))).thenAnswer(invocation -> {
            ProcesoTerapeutico proceso = invocation.getArgument(0);
            proceso.setId(20);
            return proceso;
        });
        when(entrevistaRepository.save(any(EntrevistaInicial.class))).thenAnswer(invocation -> {
            EntrevistaInicial guardada = invocation.getArgument(0);
            guardada.setId(30);
            return guardada;
        });
        when(sesionRepository.save(any(Sesion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(citaRepository.save(cita)).thenReturn(cita);

        ProcesoTerapeuticoDTO response = service.iniciarProcesoConEntrevista(
                paciente, psicologo, 8, registradoPor, entrevista);

        assertThat(response.getId()).isEqualTo(20);
        assertThat(response.getPacienteId()).isEqualTo(1);
        assertThat(response.getPsicologoId()).isEqualTo(2);
        assertThat(response.getFaseActual()).isEqualTo(1);
        assertThat(cita.getEstado()).isEqualTo("ATENDIDA");
        assertThat(entrevista.getProcesoTerapeutico().getId()).isEqualTo(20);

        ArgumentCaptor<Sesion> sesionCaptor = ArgumentCaptor.forClass(Sesion.class);
        verify(sesionRepository).save(sesionCaptor.capture());
        assertThat(sesionCaptor.getValue().getFaseSesion()).isEqualTo(1);
        assertThat(sesionCaptor.getValue().getEvolucion()).contains("Ansiedad");
        assertThat(sesionCaptor.getValue().getRegistradoPor()).isSameAs(registradoPor);
    }

    @Test
    void iniciarProcesoConEntrevistaRechazaCitaQueNoEstaEnConsulta() {
        Paciente paciente = Paciente.builder().id(1).build();
        Psicologo psicologo = Psicologo.builder().id(2).build();
        Usuario registradoPor = Usuario.builder().id(3).build();
        Cita cita = Cita.builder().id(8).estado("PENDIENTE_PAGO").build();
        EntrevistaInicial entrevista = EntrevistaInicial.builder()
                .motivoConsulta("Ansiedad")
                .build();

        when(procesoRepository.findByPacienteIdAndActivoTrue(1)).thenReturn(Optional.empty());
        when(citaRepository.findById(8)).thenReturn(Optional.of(cita));

        assertThatThrownBy(() -> service.iniciarProcesoConEntrevista(
                paciente, psicologo, 8, registradoPor, entrevista))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("consulta");

        verify(procesoRepository, never()).save(any(ProcesoTerapeutico.class));
        verify(sesionRepository, never()).save(any(Sesion.class));
    }
}
