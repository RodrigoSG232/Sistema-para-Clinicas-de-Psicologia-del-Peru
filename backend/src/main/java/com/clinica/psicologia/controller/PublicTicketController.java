package com.clinica.psicologia.controller;

import com.clinica.psicologia.dto.TicketDTO;
import com.clinica.psicologia.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/tickets")
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "legacy.business-api.enabled", havingValue = "true")
public class PublicTicketController {

    private final TicketService ticketService;

    @GetMapping("/sala")
    public Map<String, Object> pantallaSala() {
        TicketDTO actual = ticketService.obtenerTicketActual();
        List<TicketDTO> siguientes = ticketService.listarPorEstado("ESPERA").stream()
                .limit(3)
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("actual", actual);
        response.put("siguientes", siguientes);

        return response;
    }
}
