package com.clinica.psicologia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PsychologyFacadeControllerTest {

    private MockRestServiceServer scheduling;
    private PsychologyFacadeController controller;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        scheduling = MockRestServiceServer.bindTo(builder).build();
        controller = new PsychologyFacadeController(
                builder.build(), "http://scheduling.test/", "internal-secret");
        authentication = new TestingAuthenticationToken("psicologo2", null, "ROLE_PSICOLOGO");
    }

    @Test
    void agendaUsesAuthenticatedUsernameAndForwardsDownstreamResponse() {
        String responseBody = """
                {"psicologoId":2,"identitySubject":"psicologo2","citas":[]}
                """;
        scheduling.expect(requestTo(
                        "http://scheduling.test/api/scheduling/internal/psychology/agenda?date=2026-07-14"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(PsychologyFacadeController.INTERNAL_API_KEY_HEADER, "internal-secret"))
                .andExpect(header(PsychologyFacadeController.AUTHENTICATED_USERNAME_HEADER, "psicologo2"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        ResponseEntity<?> response = controller.agenda(authentication, LocalDate.of(2026, 7, 14));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(new String((byte[]) response.getBody(), StandardCharsets.UTF_8)).isEqualTo(responseBody);
        scheduling.verify();
    }

    @Test
    void statusChangeForwardsAuthenticatedUsernameBodyAndErrorStatus() {
        byte[] requestBody = "{\"estado\":\"EN_CONSULTA\"}".getBytes(StandardCharsets.UTF_8);
        String responseBody = "{\"error\":\"La cita no pertenece al psicólogo autenticado\"}";
        scheduling.expect(requestTo(
                        "http://scheduling.test/api/scheduling/internal/psychology/appointments/19/status"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(PsychologyFacadeController.INTERNAL_API_KEY_HEADER, "internal-secret"))
                .andExpect(header(PsychologyFacadeController.AUTHENTICATED_USERNAME_HEADER, "psicologo2"))
                .andExpect(content().json(new String(requestBody, StandardCharsets.UTF_8)))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseBody));

        ResponseEntity<?> response = controller.changeAppointmentStatus(authentication, 19, requestBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(new String((byte[]) response.getBody(), StandardCharsets.UTF_8)).isEqualTo(responseBody);
        scheduling.verify();
    }
}
