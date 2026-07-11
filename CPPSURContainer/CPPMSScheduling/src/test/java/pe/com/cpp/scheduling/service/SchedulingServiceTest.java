package pe.com.cpp.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.com.cpp.scheduling.api.AppointmentCreateRequest;
import pe.com.cpp.scheduling.api.AppointmentResponse;
import pe.com.cpp.scheduling.client.PatientClient;
import pe.com.cpp.scheduling.client.PatientSnapshot;
import pe.com.cpp.scheduling.domain.Appointment;
import pe.com.cpp.scheduling.domain.Psychologist;
import pe.com.cpp.scheduling.domain.PsychologistSchedule;
import pe.com.cpp.scheduling.domain.Specialty;
import pe.com.cpp.scheduling.exception.AppointmentConflictException;
import pe.com.cpp.scheduling.repository.AppointmentRepository;
import pe.com.cpp.scheduling.repository.PsychologistRepository;
import pe.com.cpp.scheduling.repository.PsychologistScheduleRepository;
import pe.com.cpp.scheduling.repository.SpecialtyRepository;

@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private PsychologistRepository psychologistRepository;
    @Mock private PsychologistScheduleRepository scheduleRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientClient patientClient;

    private SchedulingService schedulingService;
    private Psychologist psychologist;
    private Specialty specialty;
    private PsychologistSchedule schedule;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T15:00:00Z"), ZoneId.of("America/Lima"));
        schedulingService = new SchedulingService(specialtyRepository, psychologistRepository,
                scheduleRepository, appointmentRepository, patientClient, clock);

        psychologist = mock(Psychologist.class);
        specialty = mock(Specialty.class);
        schedule = mock(PsychologistSchedule.class);
        when(psychologist.getId()).thenReturn(1);
        when(psychologist.isActive()).thenReturn(true);
        when(psychologist.getSpecialty()).thenReturn(specialty);
        when(specialty.getId()).thenReturn(1);
        when(specialty.isActive()).thenReturn(true);
        when(schedule.getStartTime()).thenReturn(LocalTime.of(8, 0));
        when(schedule.getEndTime()).thenReturn(LocalTime.of(19, 0));
    }

    @Test
    void createsAppointmentWithPatientSnapshot() {
        AppointmentCreateRequest request = request();
        when(psychologist.getFullName()).thenReturn("Jose Martinez Vargas");
        when(specialty.getName()).thenReturn("Psicologia Clinica");
        when(specialty.getFee()).thenReturn(new BigDecimal("80.00"));
        when(psychologistRepository.findById(1)).thenReturn(Optional.of(psychologist));
        when(specialtyRepository.findById(1)).thenReturn(Optional.of(specialty));
        when(scheduleRepository.findByPsychologistIdAndDayOfWeekAndActiveTrue(1, 1))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.existsByPsychologistIdAndAppointmentDateAndAppointmentTimeAndStatusNotIn(
                any(), any(), any(), any())).thenReturn(false);
        when(patientClient.findById(7)).thenReturn(new PatientSnapshot(
                7, "HC-0007", "76543210", "Ana", "Torres", "Ana Torres", true));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = schedulingService.createAppointment(request);

        assertThat(response.estado()).isEqualTo("PENDIENTE_PAGO");
        assertThat(response.pacienteId()).isEqualTo(7);
        assertThat(response.paciente()).isEqualTo("Ana Torres");
        assertThat(response.monto()).isEqualByComparingTo("80.00");
        assertThat(response.creadoEn()).isEqualTo("2026-07-11T10:00:00");
        verify(patientClient).findById(7);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void rejectsOccupiedSlotBeforeCallingPatientService() {
        AppointmentCreateRequest request = request();
        when(psychologistRepository.findById(1)).thenReturn(Optional.of(psychologist));
        when(specialtyRepository.findById(1)).thenReturn(Optional.of(specialty));
        when(scheduleRepository.findByPsychologistIdAndDayOfWeekAndActiveTrue(1, 1))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.existsByPsychologistIdAndAppointmentDateAndAppointmentTimeAndStatusNotIn(
                any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> schedulingService.createAppointment(request))
                .isInstanceOf(AppointmentConflictException.class)
                .hasMessage("Esa hora ya está reservada");
        verify(patientClient, never()).findById(any());
        verify(appointmentRepository, never()).save(any());
    }

    private AppointmentCreateRequest request() {
        return new AppointmentCreateRequest(7, 1, 1,
                LocalDate.of(2026, 7, 13), LocalTime.of(10, 0));
    }
}
