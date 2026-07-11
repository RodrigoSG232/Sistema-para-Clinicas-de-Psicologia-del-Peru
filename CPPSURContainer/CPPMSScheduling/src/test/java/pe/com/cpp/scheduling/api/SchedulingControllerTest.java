package pe.com.cpp.scheduling.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import pe.com.cpp.scheduling.exception.ApiExceptionHandler;
import pe.com.cpp.scheduling.service.SchedulingService;

@ExtendWith(MockitoExtension.class)
class SchedulingControllerTest {

    @Mock
    private SchedulingService schedulingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SchedulingController(schedulingService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createsAppointmentAndReturnsLocation() throws Exception {
        AppointmentResponse response = new AppointmentResponse(
                12, LocalDate.of(2026, 7, 13), LocalTime.of(10, 0), "PENDIENTE_PAGO",
                1, "Jose Martinez Vargas", 1, "Psicologia Clinica", new BigDecimal("80.00"),
                1, "Ana Torres", "76543210", "HC-0001", LocalDateTime.of(2026, 7, 11, 10, 0));
        when(schedulingService.createAppointment(any(AppointmentCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/scheduling/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pacienteId": 1,
                                  "psicologoId": 1,
                                  "especialidadId": 1,
                                  "fecha": "2026-07-13",
                                  "hora": "10:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/scheduling/appointments/12"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.pacienteHc").value("HC-0001"))
                .andExpect(jsonPath("$.monto").value(80.00));
    }

    @Test
    void rejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/scheduling/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pacienteId").exists())
                .andExpect(jsonPath("$.psicologoId").exists())
                .andExpect(jsonPath("$.especialidadId").exists())
                .andExpect(jsonPath("$.fecha").exists())
                .andExpect(jsonPath("$.hora").exists());
    }
}
