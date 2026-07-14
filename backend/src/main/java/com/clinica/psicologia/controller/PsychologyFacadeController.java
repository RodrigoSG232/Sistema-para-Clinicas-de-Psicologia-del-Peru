package com.clinica.psicologia.controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/psychology")
public class PsychologyFacadeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PsychologyFacadeController.class);
    static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    static final String AUTHENTICATED_USERNAME_HEADER = "X-Authenticated-Username";

    private final RestClient restClient;
    private final String schedulingUrl;
    private final String internalApiKey;

    @Autowired
    public PsychologyFacadeController(
            RestClient.Builder restClientBuilder,
            @Value("${services.scheduling.base-url:http://localhost:8082}") String schedulingUrl,
            @Value("${services.internal-api-key:cpp-internal-dev-key}") String internalApiKey) {
        this(buildPatchCapableClient(restClientBuilder), schedulingUrl, internalApiKey);
    }

    PsychologyFacadeController(RestClient restClient, String schedulingUrl, String internalApiKey) {
        this.restClient = restClient;
        this.schedulingUrl = withoutTrailingSlash(schedulingUrl);
        this.internalApiKey = internalApiKey;
    }

    private static RestClient buildPatchCapableClient(RestClient.Builder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return builder.requestFactory(requestFactory).build();
    }

    @GetMapping("/agenda")
    public ResponseEntity<?> agenda(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        URI target = UriComponentsBuilder.fromUriString(schedulingUrl)
                .path("/api/scheduling/internal/psychology/agenda")
                .queryParam("date", date)
                .build()
                .toUri();
        return forward(HttpMethod.GET, target, authentication.getName(), null);
    }

    @PatchMapping("/appointments/{id}/status")
    public ResponseEntity<?> changeAppointmentStatus(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody byte[] body) {
        URI target = UriComponentsBuilder.fromUriString(schedulingUrl)
                .path("/api/scheduling/internal/psychology/appointments/{id}/status")
                .buildAndExpand(id)
                .toUri();
        return forward(HttpMethod.PATCH, target, authentication.getName(), body);
    }

    private ResponseEntity<?> forward(HttpMethod method, URI target, String subject, byte[] body) {
        try {
            RestClient.RequestBodySpec outgoing = restClient.method(method)
                    .uri(target)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .header(AUTHENTICATED_USERNAME_HEADER, subject);

            if (body != null && body.length > 0) {
                outgoing.contentType(MediaType.APPLICATION_JSON).body(body);
            }

            return outgoing.exchange((sent, received) -> {
                HttpHeaders headers = new HttpHeaders();
                if (received.getHeaders().getContentType() != null) {
                    headers.setContentType(received.getHeaders().getContentType());
                }
                return ResponseEntity.status(received.getStatusCode())
                        .headers(headers)
                        .body(readBody(received));
            });
        } catch (ResourceAccessException exception) {
            LOGGER.warn("No se pudo comunicar con Scheduling en {} {}", method, target, exception);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "El microservicio de turnos no está disponible"));
        }
    }

    private byte[] readBody(org.springframework.http.client.ClientHttpResponse response) throws IOException {
        return response.getBody().readAllBytes();
    }

    private static String withoutTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
