package pe.com.cpp.scheduling.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.com.cpp.scheduling.service.SchedulingService;

@RestController
@RequestMapping("/api/scheduling")
public class SchedulingController {

    private final SchedulingService schedulingService;

    public SchedulingController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    @GetMapping("/specialties")
    public List<SpecialtyResponse> findSpecialties() {
        return schedulingService.findSpecialties();
    }

    @GetMapping("/psychologists")
    public List<PsychologistResponse> findPsychologists(
            @RequestParam(required = false) Integer specialtyId) {
        return schedulingService.findPsychologists(specialtyId);
    }

    @GetMapping("/psychologists/{id}/availability")
    public AvailabilityResponse findAvailability(
            @PathVariable Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return schedulingService.findAvailability(id, date);
    }

    @PostMapping("/appointments")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody AppointmentCreateRequest request) {
        AppointmentResponse appointment = schedulingService.createAppointment(request);
        return ResponseEntity.created(URI.create("/api/scheduling/appointments/" + appointment.id()))
                .body(appointment);
    }

    @GetMapping("/appointments/patient/{patientId}")
    public List<AppointmentResponse> findByPatient(@PathVariable Integer patientId) {
        return schedulingService.findByPatient(patientId);
    }

    @GetMapping("/appointments/week")
    public List<AppointmentResponse> findByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return schedulingService.findByDateRange(start, end);
    }

    @PatchMapping("/appointments/{id}/status")
    public AppointmentResponse changeStatus(
            @PathVariable Integer id,
            @Valid @RequestBody AppointmentStatusRequest request) {
        return schedulingService.changeStatus(id, request.estado());
    }
}
