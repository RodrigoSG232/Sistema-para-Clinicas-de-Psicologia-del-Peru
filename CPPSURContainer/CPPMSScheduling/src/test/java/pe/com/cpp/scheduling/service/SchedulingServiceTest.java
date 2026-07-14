package pe.com.cpp.scheduling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.com.cpp.scheduling.api.AppointmentCreateRequest;
import pe.com.cpp.scheduling.api.AppointmentResponse;
import pe.com.cpp.scheduling.api.AvailabilityResponse;
import pe.com.cpp.scheduling.api.PsychologyAgendaResponse;
import pe.com.cpp.scheduling.client.PatientClient;
import pe.com.cpp.scheduling.client.PatientSnapshot;
import pe.com.cpp.scheduling.domain.Appointment;
import pe.com.cpp.scheduling.domain.AppointmentStatus;
import pe.com.cpp.scheduling.domain.Psychologist;
import pe.com.cpp.scheduling.domain.PsychologistSchedule;
import pe.com.cpp.scheduling.domain.Specialty;
import pe.com.cpp.scheduling.exception.AppointmentConflictException;
import pe.com.cpp.scheduling.exception.ForbiddenOperationException;
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
        lenient().when(psychologist.getId()).thenReturn(1);
        lenient().when(psychologist.isActive()).thenReturn(true);
        lenient().when(schedule.getStartTime()).thenReturn(LocalTime.of(8, 0));
        lenient().when(schedule.getEndTime()).thenReturn(LocalTime.of(19, 0));
    }

    @Test
    void createsAppointmentWithPatientSnapshot() {
        AppointmentCreateRequest request = request();
        stubActiveSpecialty();
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
        stubActiveSpecialty();
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

    @Test
    void availabilityExcludesOccupiedSlots() {
        LocalDate date = LocalDate.of(2026, 7, 13);
        when(psychologistRepository.findById(1)).thenReturn(Optional.of(psychologist));
        when(scheduleRepository.findByPsychologistIdAndDayOfWeekAndActiveTrue(1, 1))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.findOccupiedTimes(any(), any(), any()))
                .thenReturn(List.of(LocalTime.of(10, 0)));

        AvailabilityResponse response = schedulingService.findAvailability(1, date);

        assertThat(response.horasDisponibles()).contains("08:00", "09:00", "11:00");
        assertThat(response.horasDisponibles()).doesNotContain("10:00");
        assertThat(response.horasOcupadas()).containsExactly("10:00");
    }

    @Test
    void ownAgendaReturnsOnlyAppointmentsForAuthenticatedPsychologist() {
        LocalDate date = LocalDate.of(2026, 7, 13);
        Psychologist rosa = mock(Psychologist.class);
        Appointment rosasAppointment = mock(Appointment.class);

        when(rosa.getId()).thenReturn(2);
        when(rosa.getIdentitySubject()).thenReturn("psicologo2");
        when(rosa.getFullName()).thenReturn("Rosa Quispe Flores");
        when(rosa.isActive()).thenReturn(true);
        stubAppointmentResponse(rosasAppointment, rosa, AppointmentStatus.ON_FLOOR, date);
        when(psychologistRepository.findByIdentitySubjectIgnoreCase("psicologo2"))
                .thenReturn(Optional.of(rosa));
        when(appointmentRepository.findByPsychologistIdAndAppointmentDateOrderByAppointmentTimeAsc(2, date))
                .thenReturn(List.of(rosasAppointment));

        PsychologyAgendaResponse response = schedulingService
                .findOwnPsychologyAgenda(" psicologo2 ", date);

        assertThat(response.psicologoId()).isEqualTo(2);
        assertThat(response.identitySubject()).isEqualTo("psicologo2");
        assertThat(response.nombreCompleto()).isEqualTo("Rosa Quispe Flores");
        assertThat(response.citas()).singleElement()
                .satisfies(appointment -> {
                    assertThat(appointment.psicologoId()).isEqualTo(2);
                    assertThat(appointment.estado()).isEqualTo("EN_PISO");
                });
        verify(appointmentRepository)
                .findByPsychologistIdAndAppointmentDateOrderByAppointmentTimeAsc(2, date);
        verify(appointmentRepository, never())
                .findByAppointmentDateBetweenOrderByAppointmentDateAscAppointmentTimeAsc(any(), any());
    }

    @Test
    void ownStatusChangeMovesOwnedOnFloorAppointmentToConsultation() {
        LocalDate date = LocalDate.of(2026, 7, 13);
        Appointment appointment = mock(Appointment.class);
        when(psychologist.getFullName()).thenReturn("Jose Martinez Vargas");
        stubAppointmentResponse(appointment, psychologist, null, date);
        when(appointment.getStatus()).thenReturn(
                AppointmentStatus.ON_FLOOR,
                AppointmentStatus.IN_CONSULTATION);
        when(psychologistRepository.findByIdentitySubjectIgnoreCase("psicologo"))
                .thenReturn(Optional.of(psychologist));
        when(appointmentRepository.findByIdAndPsychologistId(12, 1))
                .thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(appointment)).thenReturn(appointment);

        AppointmentResponse response = schedulingService
                .changeOwnAppointmentStatus("psicologo", 12, "EN_CONSULTA");

        assertThat(response.estado()).isEqualTo("EN_CONSULTA");
        assertThat(response.psicologoId()).isEqualTo(1);
        verify(appointment).changeStatus(AppointmentStatus.IN_CONSULTATION);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void ownStatusChangeIsIdempotentForRetries() {
        LocalDate date = LocalDate.of(2026, 7, 13);
        Appointment appointment = mock(Appointment.class);
        when(psychologist.getFullName()).thenReturn("Jose Martinez Vargas");
        stubAppointmentResponse(appointment, psychologist, AppointmentStatus.IN_CONSULTATION, date);
        when(psychologistRepository.findByIdentitySubjectIgnoreCase("psicologo"))
                .thenReturn(Optional.of(psychologist));
        when(appointmentRepository.findByIdAndPsychologistId(12, 1))
                .thenReturn(Optional.of(appointment));

        AppointmentResponse response = schedulingService
                .changeOwnAppointmentStatus("psicologo", 12, "EN_CONSULTA");

        assertThat(response.estado()).isEqualTo("EN_CONSULTA");
        verify(appointment, never()).changeStatus(any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void ownStatusChangeCannotCancelAppointment() {
        Appointment appointment = mock(Appointment.class);
        when(psychologistRepository.findByIdentitySubjectIgnoreCase("psicologo"))
                .thenReturn(Optional.of(psychologist));
        when(appointmentRepository.findByIdAndPsychologistId(12, 1))
                .thenReturn(Optional.of(appointment));
        when(appointment.getStatus()).thenReturn(AppointmentStatus.ON_FLOOR);

        assertThatThrownBy(() -> schedulingService
                .changeOwnAppointmentStatus("psicologo", 12, "CANCELADA"))
                .isInstanceOf(pe.com.cpp.scheduling.exception.BusinessRuleException.class)
                .hasMessage("Transición de estado no permitida para Psicología: EN_PISO -> CANCELADA");
        verify(appointment, never()).changeStatus(any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void ownStatusChangeRejectsAppointmentAssignedToAnotherPsychologist() {
        when(psychologistRepository.findByIdentitySubjectIgnoreCase("psicologo"))
                .thenReturn(Optional.of(psychologist));
        when(appointmentRepository.findByIdAndPsychologistId(99, 1))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> schedulingService
                .changeOwnAppointmentStatus("psicologo", 99, "EN_CONSULTA"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("La cita no pertenece al psicólogo autenticado");
        verify(appointmentRepository, never()).save(any());
    }

    private AppointmentCreateRequest request() {
        return new AppointmentCreateRequest(7, 1, 1,
                LocalDate.of(2026, 7, 13), LocalTime.of(10, 0));
    }

    private void stubActiveSpecialty() {
        when(psychologist.getSpecialty()).thenReturn(specialty);
        when(specialty.getId()).thenReturn(1);
        when(specialty.isActive()).thenReturn(true);
    }

    private void stubAppointmentResponse(Appointment appointment, Psychologist owner,
            AppointmentStatus status, LocalDate date) {
        when(appointment.getId()).thenReturn(12);
        when(appointment.getAppointmentDate()).thenReturn(date);
        when(appointment.getAppointmentTime()).thenReturn(LocalTime.of(10, 0));
        if (status != null) {
            when(appointment.getStatus()).thenReturn(status);
        }
        when(appointment.getPsychologist()).thenReturn(owner);
        when(appointment.getSpecialty()).thenReturn(specialty);
        when(appointment.getPatientId()).thenReturn(7);
        when(appointment.getPatientName()).thenReturn("Paciente Prueba");
        when(appointment.getPatientDni()).thenReturn("76543210");
        when(appointment.getPatientHistoryNumber()).thenReturn("HC-0007");
        when(appointment.getCreatedAt()).thenReturn(java.time.LocalDateTime.of(2026, 7, 11, 10, 0));
        when(specialty.getId()).thenReturn(1);
        when(specialty.getName()).thenReturn("Psicologia Clinica");
        when(specialty.getFee()).thenReturn(new BigDecimal("80.00"));
    }
}
