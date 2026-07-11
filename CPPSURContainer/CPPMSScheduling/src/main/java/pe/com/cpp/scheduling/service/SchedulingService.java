package pe.com.cpp.scheduling.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.cpp.scheduling.api.AppointmentCreateRequest;
import pe.com.cpp.scheduling.api.AppointmentResponse;
import pe.com.cpp.scheduling.api.AvailabilityResponse;
import pe.com.cpp.scheduling.api.PsychologistResponse;
import pe.com.cpp.scheduling.api.SpecialtyResponse;
import pe.com.cpp.scheduling.client.PatientClient;
import pe.com.cpp.scheduling.client.PatientSnapshot;
import pe.com.cpp.scheduling.domain.Appointment;
import pe.com.cpp.scheduling.domain.AppointmentStatus;
import pe.com.cpp.scheduling.domain.Psychologist;
import pe.com.cpp.scheduling.domain.PsychologistSchedule;
import pe.com.cpp.scheduling.domain.Specialty;
import pe.com.cpp.scheduling.exception.AppointmentConflictException;
import pe.com.cpp.scheduling.exception.BusinessRuleException;
import pe.com.cpp.scheduling.exception.ResourceNotFoundException;
import pe.com.cpp.scheduling.repository.AppointmentRepository;
import pe.com.cpp.scheduling.repository.PsychologistRepository;
import pe.com.cpp.scheduling.repository.PsychologistScheduleRepository;
import pe.com.cpp.scheduling.repository.SpecialtyRepository;

@Service
public class SchedulingService {

    private static final Set<AppointmentStatus> RELEASED_SLOT_STATUSES =
            EnumSet.of(AppointmentStatus.CANCELLED, AppointmentStatus.ATTENDED);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED_TRANSITIONS = Map.of(
            AppointmentStatus.PENDING_PAYMENT, EnumSet.of(AppointmentStatus.PAID, AppointmentStatus.CANCELLED),
            AppointmentStatus.PAID, EnumSet.of(AppointmentStatus.ON_FLOOR, AppointmentStatus.CANCELLED),
            AppointmentStatus.ON_FLOOR, EnumSet.of(AppointmentStatus.IN_CONSULTATION, AppointmentStatus.CANCELLED),
            AppointmentStatus.IN_CONSULTATION, EnumSet.of(AppointmentStatus.ATTENDED));

    private final SpecialtyRepository specialtyRepository;
    private final PsychologistRepository psychologistRepository;
    private final PsychologistScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientClient patientClient;
    private final Clock clock;

    public SchedulingService(SpecialtyRepository specialtyRepository,
            PsychologistRepository psychologistRepository,
            PsychologistScheduleRepository scheduleRepository,
            AppointmentRepository appointmentRepository,
            PatientClient patientClient,
            Clock clock) {
        this.specialtyRepository = specialtyRepository;
        this.psychologistRepository = psychologistRepository;
        this.scheduleRepository = scheduleRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientClient = patientClient;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SpecialtyResponse> findSpecialties() {
        return specialtyRepository.findByActiveTrueOrderByName().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PsychologistResponse> findPsychologists(Integer specialtyId) {
        List<Psychologist> psychologists = specialtyId == null
                ? psychologistRepository.findByActiveTrueOrderByLastNamesAscFirstNamesAsc()
                : psychologistRepository.findBySpecialtyIdAndActiveTrueOrderByLastNamesAscFirstNamesAsc(specialtyId);
        return psychologists.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse findAvailability(Integer psychologistId, java.time.LocalDate date) {
        Psychologist psychologist = activePsychologist(psychologistId);
        PsychologistSchedule schedule = scheduleRepository
                .findByPsychologistIdAndDayOfWeekAndActiveTrue(psychologist.getId(), date.getDayOfWeek().getValue())
                .orElse(null);
        if (schedule == null) {
            return new AvailabilityResponse(psychologistId, date, List.of(), List.of());
        }

        List<LocalTime> occupied = appointmentRepository.findOccupiedTimes(
                psychologistId, date, RELEASED_SLOT_STATUSES);
        List<String> occupiedFormatted = occupied.stream().sorted().map(TIME_FORMAT::format).toList();
        List<String> available = slots(schedule).filter(slot -> !occupied.contains(slot))
                .map(TIME_FORMAT::format).toList();
        return new AvailabilityResponse(psychologistId, date, available, occupiedFormatted);
    }

    @Transactional
    public AppointmentResponse createAppointment(AppointmentCreateRequest request) {
        Psychologist psychologist = activePsychologist(request.psicologoId());
        Specialty specialty = activeSpecialty(request.especialidadId());
        if (!psychologist.getSpecialty().getId().equals(specialty.getId())) {
            throw new BusinessRuleException("El psicólogo no pertenece a la especialidad seleccionada");
        }

        PsychologistSchedule schedule = scheduleRepository
                .findByPsychologistIdAndDayOfWeekAndActiveTrue(
                        psychologist.getId(), request.fecha().getDayOfWeek().getValue())
                .orElseThrow(() -> new BusinessRuleException("El psicólogo no atiende en la fecha seleccionada"));
        if (slots(schedule).noneMatch(request.hora()::equals)) {
            throw new BusinessRuleException("La hora no pertenece al horario del psicólogo");
        }
        if (appointmentRepository.existsByPsychologistIdAndAppointmentDateAndAppointmentTimeAndStatusNotIn(
                psychologist.getId(), request.fecha(), request.hora(), RELEASED_SLOT_STATUSES)) {
            throw new AppointmentConflictException("Esa hora ya está reservada");
        }

        PatientSnapshot patient = patientClient.findById(request.pacienteId());
        if (!Boolean.TRUE.equals(patient.activo())) {
            throw new BusinessRuleException("No se puede agendar una cita para un paciente inactivo");
        }

        Appointment appointment = new Appointment(
                patient.id(), patient.nombreCompleto(), patient.dni(), patient.numeroHistoria(),
                psychologist, specialty, request.fecha(), request.hora(), LocalDateTime.now(clock));
        return toResponse(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findByPatient(Integer patientId) {
        return appointmentRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findAppointmentById(Integer appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findByDateRange(java.time.LocalDate start, java.time.LocalDate end) {
        if (end.isBefore(start)) {
            throw new BusinessRuleException("La fecha final no puede ser anterior a la fecha inicial");
        }
        return appointmentRepository
                .findByAppointmentDateBetweenOrderByAppointmentDateAscAppointmentTimeAsc(start, end)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public AppointmentResponse changeStatus(Integer appointmentId, String externalStatus) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));
        AppointmentStatus newStatus = fromExternalStatus(externalStatus);
        if (appointment.getStatus() == newStatus) {
            return toResponse(appointment);
        }
        Set<AppointmentStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(appointment.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new BusinessRuleException("Transición de estado no permitida: "
                    + toExternalStatus(appointment.getStatus()) + " -> " + externalStatus);
        }
        appointment.changeStatus(newStatus);
        return toResponse(appointmentRepository.save(appointment));
    }

    private Stream<LocalTime> slots(PsychologistSchedule schedule) {
        return Stream.iterate(schedule.getStartTime(), time -> time.isBefore(schedule.getEndTime()),
                time -> time.plusHours(1));
    }

    private Psychologist activePsychologist(Integer id) {
        Psychologist psychologist = psychologistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Psicólogo no encontrado"));
        if (!psychologist.isActive()) {
            throw new BusinessRuleException("El psicólogo se encuentra inactivo");
        }
        return psychologist;
    }

    private Specialty activeSpecialty(Integer id) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada"));
        if (!specialty.isActive()) {
            throw new BusinessRuleException("La especialidad se encuentra inactiva");
        }
        return specialty;
    }

    private SpecialtyResponse toResponse(Specialty specialty) {
        return new SpecialtyResponse(specialty.getId(), specialty.getName(), specialty.getDescription(), specialty.getFee());
    }

    private PsychologistResponse toResponse(Psychologist psychologist) {
        return new PsychologistResponse(psychologist.getId(), psychologist.getFirstNames(), psychologist.getLastNames(),
                psychologist.getFullName(), psychologist.getLicenseNumber(), psychologist.getSpecialty().getId(),
                psychologist.getSpecialty().getName());
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(appointment.getId(), appointment.getAppointmentDate(),
                appointment.getAppointmentTime(), toExternalStatus(appointment.getStatus()),
                appointment.getPsychologist().getId(), appointment.getPsychologist().getFullName(),
                appointment.getSpecialty().getId(), appointment.getSpecialty().getName(),
                appointment.getSpecialty().getFee(), appointment.getPatientId(), appointment.getPatientName(),
                appointment.getPatientDni(), appointment.getPatientHistoryNumber(), appointment.getCreatedAt());
    }

    private AppointmentStatus fromExternalStatus(String status) {
        return switch (status == null ? "" : status.trim().toUpperCase()) {
            case "PENDIENTE_PAGO" -> AppointmentStatus.PENDING_PAYMENT;
            case "PAGADA" -> AppointmentStatus.PAID;
            case "EN_PISO" -> AppointmentStatus.ON_FLOOR;
            case "EN_CONSULTA" -> AppointmentStatus.IN_CONSULTATION;
            case "ATENDIDA" -> AppointmentStatus.ATTENDED;
            case "CANCELADA" -> AppointmentStatus.CANCELLED;
            default -> throw new BusinessRuleException("Estado de cita no válido");
        };
    }

    private String toExternalStatus(AppointmentStatus status) {
        return switch (status) {
            case PENDING_PAYMENT -> "PENDIENTE_PAGO";
            case PAID -> "PAGADA";
            case ON_FLOOR -> "EN_PISO";
            case IN_CONSULTATION -> "EN_CONSULTA";
            case ATTENDED -> "ATENDIDA";
            case CANCELLED -> "CANCELADA";
        };
    }
}
