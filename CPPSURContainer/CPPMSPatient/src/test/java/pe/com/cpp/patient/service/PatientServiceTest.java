package pe.com.cpp.patient.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.com.cpp.patient.api.PatientCreateRequest;
import pe.com.cpp.patient.api.PatientResponse;
import pe.com.cpp.patient.domain.HistoryNumberSequence;
import pe.com.cpp.patient.domain.Patient;
import pe.com.cpp.patient.exception.DuplicatePatientException;
import pe.com.cpp.patient.repository.HistoryNumberSequenceRepository;
import pe.com.cpp.patient.repository.PatientRepository;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private HistoryNumberSequenceRepository sequenceRepository;

    private PatientService patientService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-10T15:30:00Z"),
                ZoneId.of("America/Lima"));
        patientService = new PatientService(patientRepository, sequenceRepository, clock);
    }

    @Test
    void createsPatientWithAtomicHistoryNumber() {
        PatientCreateRequest request = validRequest();
        HistoryNumberSequence sequence = mock(HistoryNumberSequence.class);

        when(patientRepository.existsByDni("76543210")).thenReturn(false);
        when(sequenceRepository.findByNameForUpdate(HistoryNumberSequence.PATIENT_SEQUENCE))
                .thenReturn(Optional.of(sequence));
        when(sequence.takeNextValue()).thenReturn(7);
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientResponse response = patientService.create(request);

        assertThat(response.numeroHistoria()).isEqualTo("HC-0007");
        assertThat(response.dni()).isEqualTo("76543210");
        assertThat(response.nombreCompleto()).isEqualTo("Ana Torres");
        assertThat(response.fechaApertura()).isEqualTo("2026-07-10T10:30:00");
        verify(sequence).takeNextValue();
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    void rejectsDuplicatedDniBeforeTakingSequence() {
        PatientCreateRequest request = validRequest();
        when(patientRepository.existsByDni("76543210")).thenReturn(true);

        assertThatThrownBy(() -> patientService.create(request))
                .isInstanceOf(DuplicatePatientException.class)
                .hasMessage("Ya existe un paciente con ese DNI");
    }

    private PatientCreateRequest validRequest() {
        return new PatientCreateRequest(
                "76543210",
                " Ana ",
                " Torres ",
                LocalDate.of(1994, 3, 15),
                "F",
                "987654321",
                "ana@example.com",
                "Lima");
    }

}
