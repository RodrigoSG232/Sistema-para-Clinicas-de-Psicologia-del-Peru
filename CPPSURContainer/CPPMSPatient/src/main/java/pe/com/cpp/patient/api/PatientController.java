package pe.com.cpp.patient.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import pe.com.cpp.patient.service.PatientService;

@Validated
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody PatientCreateRequest request) {
        PatientResponse patient = patientService.create(request);
        return ResponseEntity.created(URI.create("/api/patients/" + patient.id())).body(patient);
    }

    @GetMapping("/search")
    public List<PatientResponse> search(
            @RequestParam @NotBlank(message = "El criterio de búsqueda es obligatorio") String q) {
        return patientService.search(q);
    }

    @GetMapping("/{id}")
    public PatientResponse findById(@PathVariable Integer id) {
        return patientService.findById(id);
    }

    @GetMapping("/dni/{dni}")
    public PatientResponse findByDni(
            @PathVariable @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos") String dni) {
        return patientService.findByDni(dni);
    }
}
