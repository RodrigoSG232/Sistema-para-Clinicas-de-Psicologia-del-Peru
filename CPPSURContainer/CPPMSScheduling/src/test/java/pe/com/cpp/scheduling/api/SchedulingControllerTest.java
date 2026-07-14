package pe.com.cpp.scheduling.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import pe.com.cpp.scheduling.exception.ApiExceptionHandler;
import pe.com.cpp.scheduling.config.InternalPsychologyAccessFilter;
import pe.com.cpp.scheduling.exception.ForbiddenOperationException;
import pe.com.cpp.scheduling.service.SchedulingService;

@ExtendWith(MockitoExtension.class)
class SchedulingControllerTest {

    private static final String API_KEY = "test-internal-key";

    @Mock
    private SchedulingService schedulingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SchedulingController(schedulingService))
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new InternalPsychologyAccessFilter(API_KEY))
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

    @Test
    void returnsAgendaForAuthenticatedPsychologist() throws Exception {
        LocalDate date = LocalDate.of(2026, 7, 13);
        PsychologyAgendaResponse agenda = new PsychologyAgendaResponse(
                2, "psicologo2", "Rosa Quispe Flores", date,
                List.of(appointmentResponse(22, 2, "Rosa Quispe Flores", "EN_PISO")));
        when(schedulingService.findOwnPsychologyAgenda("psicologo2", date)).thenReturn(agenda);

        mockMvc.perform(get("/api/scheduling/internal/psychology/agenda")
                        .param("date", "2026-07-13")
                        .header(InternalPsychologyAccessFilter.INTERNAL_API_KEY_HEADER, API_KEY)
                        .header(InternalPsychologyAccessFilter.AUTHENTICATED_USERNAME_HEADER, "psicologo2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.psicologoId").value(2))
                .andExpect(jsonPath("$.identitySubject").value("psicologo2"))
                .andExpect(jsonPath("$.citas.length()").value(1))
                .andExpect(jsonPath("$.citas[0].psicologoId").value(2))
                .andExpect(jsonPath("$.citas[0].estado").value("EN_PISO"));
    }

    @Test
    void rejectsInternalAgendaWithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/scheduling/internal/psychology/agenda")
                        .param("date", "2026-07-13")
                        .header(InternalPsychologyAccessFilter.AUTHENTICATED_USERNAME_HEADER, "psicologo2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso interno no autorizado"));

        verify(schedulingService, never()).findOwnPsychologyAgenda(any(), any());
    }

    @Test
    void rejectsInternalAgendaWithIncorrectApiKey() throws Exception {
        mockMvc.perform(get("/api/scheduling/internal/psychology/agenda")
                        .param("date", "2026-07-13")
                        .header(InternalPsychologyAccessFilter.INTERNAL_API_KEY_HEADER, "incorrect-key")
                        .header(InternalPsychologyAccessFilter.AUTHENTICATED_USERNAME_HEADER, "psicologo2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso interno no autorizado"));

        verify(schedulingService, never()).findOwnPsychologyAgenda(any(), any());
    }

    @Test
    void rejectsInternalAgendaWithoutAuthenticatedUsername() throws Exception {
        mockMvc.perform(get("/api/scheduling/internal/psychology/agenda")
                        .param("date", "2026-07-13")
                        .header(InternalPsychologyAccessFilter.INTERNAL_API_KEY_HEADER, API_KEY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No se recibió la identidad autenticada"));

        verify(schedulingService, never()).findOwnPsychologyAgenda(any(), any());
    }

    @Test
    void changesStatusOnlyThroughAuthenticatedPsychologistEndpoint() throws Exception {
        AppointmentResponse response = appointmentResponse(22, 2, "Rosa Quispe Flores", "EN_CONSULTA");
        when(schedulingService.changeOwnAppointmentStatus("psicologo2", 22, "EN_CONSULTA"))
                .thenReturn(response);

        mockMvc.perform(patch("/api/scheduling/internal/psychology/appointments/22/status")
                        .header(InternalPsychologyAccessFilter.INTERNAL_API_KEY_HEADER, API_KEY)
                        .header(InternalPsychologyAccessFilter.AUTHENTICATED_USERNAME_HEADER, "psicologo2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_CONSULTA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(22))
                .andExpect(jsonPath("$.psicologoId").value(2))
                .andExpect(jsonPath("$.estado").value("EN_CONSULTA"));
    }

    @Test
    void rejectsStatusChangeForAppointmentAssignedToAnotherPsychologist() throws Exception {
        when(schedulingService.changeOwnAppointmentStatus("psicologo2", 99, "EN_CONSULTA"))
                .thenThrow(new ForbiddenOperationException(
                        "La cita no pertenece al psicólogo autenticado"));

        mockMvc.perform(patch("/api/scheduling/internal/psychology/appointments/99/status")
                        .header(InternalPsychologyAccessFilter.INTERNAL_API_KEY_HEADER, API_KEY)
                        .header(InternalPsychologyAccessFilter.AUTHENTICATED_USERNAME_HEADER, "psicologo2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_CONSULTA\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value("La cita no pertenece al psicólogo autenticado"));
    }

    private AppointmentResponse appointmentResponse(Integer id, Integer psychologistId,
            String psychologistName, String status) {
        return new AppointmentResponse(
                id, LocalDate.of(2026, 7, 13), LocalTime.of(10, 0), status,
                psychologistId, psychologistName, 1, "Psicologia Clinica", new BigDecimal("80.00"),
                1, "Ana Torres", "76543210", "HC-0001",
                LocalDateTime.of(2026, 7, 11, 10, 0));
    }
}
