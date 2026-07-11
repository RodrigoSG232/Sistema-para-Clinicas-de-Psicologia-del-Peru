package pe.com.cpp.patient.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import pe.com.cpp.patient.exception.ApiExceptionHandler;
import pe.com.cpp.patient.service.PatientService;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    @Mock
    private PatientService patientService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PatientController(patientService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createsPatientAndReturnsLocation() throws Exception {
        PatientResponse response = new PatientResponse(
                1,
                "HC-0001",
                "76543210",
                "Ana",
                "Torres",
                "Ana Torres",
                LocalDate.of(1994, 3, 15),
                "F",
                "987654321",
                "ana@example.com",
                "Lima",
                LocalDateTime.of(2026, 7, 10, 10, 30),
                true);
        when(patientService.create(any(PatientCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dni": "76543210",
                                  "nombres": "Ana",
                                  "apellidos": "Torres",
                                  "fechaNacimiento": "1994-03-15",
                                  "sexo": "F",
                                  "telefono": "987654321",
                                  "email": "ana@example.com",
                                  "direccion": "Lima"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/patients/1"))
                .andExpect(jsonPath("$.numeroHistoria").value("HC-0001"));
    }

    @Test
    void rejectsInvalidPatientData() throws Exception {
        mockMvc.perform(post("/api/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dni": "123",
                                  "nombres": "",
                                  "apellidos": "Torres",
                                  "fechaNacimiento": "2099-01-01",
                                  "sexo": "X"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dni").exists())
                .andExpect(jsonPath("$.nombres").exists())
                .andExpect(jsonPath("$.fechaNacimiento").exists())
                .andExpect(jsonPath("$.sexo").exists());
    }
}
