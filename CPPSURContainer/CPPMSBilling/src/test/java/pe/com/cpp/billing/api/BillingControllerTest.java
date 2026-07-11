package pe.com.cpp.billing.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import pe.com.cpp.billing.exception.ApiExceptionHandler;
import pe.com.cpp.billing.service.BillingService;

@ExtendWith(MockitoExtension.class)
class BillingControllerTest {

    @Mock
    private BillingService billingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BillingController(billingService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createsDebtFromAppointmentAndReturnsLocation() throws Exception {
        DebtResponse response = new DebtResponse(
                5, 1, "Ana Torres", "76543210", "HC-0001", 12,
                "Gastos de cita", "Psicologia Clinica", new BigDecimal("80.00"),
                "PENDIENTE", LocalDateTime.of(2026, 7, 11, 11, 0));
        when(billingService.createFromAppointment(12)).thenReturn(response);

        mockMvc.perform(post("/api/billing/debts/from-appointment/12"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/billing/debts/5"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.pacienteHc").value("HC-0001"))
                .andExpect(jsonPath("$.monto").value(80.00));
    }

    @Test
    void rejectsPaymentWithoutPaymentMethod() throws Exception {
        mockMvc.perform(post("/api/billing/payments/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"BOLETA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.medioPago").exists());
    }
}
