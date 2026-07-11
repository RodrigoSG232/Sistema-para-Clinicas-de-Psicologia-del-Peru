package pe.com.cpp.patient.service;

import static pe.com.cpp.patient.domain.HistoryNumberSequence.PATIENT_SEQUENCE;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.com.cpp.patient.api.PatientCreateRequest;
import pe.com.cpp.patient.api.PatientResponse;
import pe.com.cpp.patient.domain.HistoryNumberSequence;
import pe.com.cpp.patient.domain.Patient;
import pe.com.cpp.patient.exception.DuplicatePatientException;
import pe.com.cpp.patient.exception.ResourceNotFoundException;
import pe.com.cpp.patient.repository.HistoryNumberSequenceRepository;
import pe.com.cpp.patient.repository.PatientRepository;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final HistoryNumberSequenceRepository sequenceRepository;
    private final Clock clock;

    public PatientService(PatientRepository patientRepository,
            HistoryNumberSequenceRepository sequenceRepository, Clock clock) {
        this.patientRepository = patientRepository;
        this.sequenceRepository = sequenceRepository;
        this.clock = clock;
    }

    @Transactional
    public PatientResponse create(PatientCreateRequest request) {
        String dni = request.dni().trim();
        if (patientRepository.existsByDni(dni)) {
            throw new DuplicatePatientException("Ya existe un paciente con ese DNI");
        }

        HistoryNumberSequence sequence = sequenceRepository.findByNameForUpdate(PATIENT_SEQUENCE)
                .orElseThrow(() -> new IllegalStateException("No se configuró la secuencia de historias clínicas"));
        String historyNumber = "HC-" + String.format("%04d", sequence.takeNextValue());

        Patient patient = new Patient(
                historyNumber,
                dni,
                request.nombres().trim(),
                request.apellidos().trim(),
                request.fechaNacimiento(),
                request.sexo().trim(),
                trimToNull(request.telefono()),
                trimToNull(request.email()),
                trimToNull(request.direccion()),
                LocalDateTime.now(clock));

        return toResponse(patientRepository.save(patient));
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> search(String query) {
        return patientRepository.search(query.trim()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse findById(Integer id) {
        return patientRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
    }

    @Transactional(readOnly = true)
    public PatientResponse findByDni(String dni) {
        return patientRepository.findByDni(dni)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
    }

    private PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getHistoryNumber(),
                patient.getDni(),
                patient.getFirstNames(),
                patient.getLastNames(),
                patient.getFirstNames() + " " + patient.getLastNames(),
                patient.getBirthDate(),
                patient.getSex(),
                patient.getPhone(),
                patient.getEmail(),
                patient.getAddress(),
                patient.getCreatedAt(),
                patient.isActive());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
