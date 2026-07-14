package com.clinica.psicologia.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Map;

@RestController
public class MicroserviceProxyController {

    private final RestClient restClient = RestClient.create();
    private final String clinicalUrl;
    private final String queueUrl;
    private final String patientUrl;
    private final String schedulingUrl;
    private final String billingUrl;
    private final String internalApiKey;

    public MicroserviceProxyController(
            @Value("${services.clinical.base-url:http://localhost:8084}") String clinicalUrl,
            @Value("${services.queue.base-url:http://localhost:8085}") String queueUrl,
            @Value("${services.patient.base-url:http://localhost:8081}") String patientUrl,
            @Value("${services.scheduling.base-url:http://localhost:8082}") String schedulingUrl,
            @Value("${services.billing.base-url:http://localhost:8083}") String billingUrl,
            @Value("${services.internal-api-key:cpp-internal-dev-key}") String internalApiKey) {
        this.clinicalUrl = withoutTrailingSlash(clinicalUrl);
        this.queueUrl = withoutTrailingSlash(queueUrl);
        this.patientUrl = withoutTrailingSlash(patientUrl);
        this.schedulingUrl = withoutTrailingSlash(schedulingUrl);
        this.billingUrl = withoutTrailingSlash(billingUrl);
        this.internalApiKey = internalApiKey;
    }

    @RequestMapping("/api/clinical/**")
    public ResponseEntity<?> clinical(HttpServletRequest request,
                                      @RequestBody(required = false) byte[] body) {
        return forward(clinicalUrl, request, body);
    }

    @RequestMapping("/api/queue/**")
    public ResponseEntity<?> queue(HttpServletRequest request,
                                   @RequestBody(required = false) byte[] body) {
        return forward(queueUrl, request, body);
    }

    @RequestMapping("/api/patients/**")
    public ResponseEntity<?> patients(HttpServletRequest request,
                                      @RequestBody(required = false) byte[] body) {
        return forward(patientUrl, request, body);
    }

    @RequestMapping("/api/scheduling/**")
    public ResponseEntity<?> scheduling(HttpServletRequest request,
                                        @RequestBody(required = false) byte[] body) {
        return forward(schedulingUrl, request, body);
    }

    @RequestMapping("/api/billing/**")
    public ResponseEntity<?> billing(HttpServletRequest request,
                                     @RequestBody(required = false) byte[] body) {
        return forward(billingUrl, request, body);
    }

    private ResponseEntity<?> forward(String serviceUrl, HttpServletRequest request, byte[] body) {
        String target = serviceUrl + request.getRequestURI();
        if (request.getQueryString() != null) {
            target += "?" + request.getQueryString();
        }

        try {
            RestClient.RequestBodySpec outgoing = restClient
                    .method(HttpMethod.valueOf(request.getMethod()))
                    .uri(URI.create(target));

            String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
            String accept = request.getHeader(HttpHeaders.ACCEPT);
            if (contentType != null) outgoing.header(HttpHeaders.CONTENT_TYPE, contentType);
            if (accept != null) outgoing.header(HttpHeaders.ACCEPT, accept);
            outgoing.header("X-Internal-Api-Key", internalApiKey);
            if (body != null && body.length > 0) outgoing.body(body);

            return outgoing.exchange((sent, received) -> {
                HttpHeaders headers = new HttpHeaders();
                if (received.getHeaders().getContentType() != null) {
                    headers.setContentType(received.getHeaders().getContentType());
                }
                String disposition = received.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
                if (disposition != null) headers.set(HttpHeaders.CONTENT_DISPOSITION, disposition);
                return ResponseEntity.status(received.getStatusCode())
                        .headers(headers)
                        .body(received.getBody().readAllBytes());
            });
        } catch (ResourceAccessException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "El microservicio requerido no está disponible"));
        }
    }

    private static String withoutTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
