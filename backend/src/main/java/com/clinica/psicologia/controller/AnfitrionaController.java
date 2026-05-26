package com.clinica.psicologia.controller;

import com.clinica.psicologia.dto.TicketDTO;
import com.clinica.psicologia.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/anfitriona")
@RequiredArgsConstructor
public class AnfitrionaController {

    private final TicketService ticketService;

    @PostMapping("/tickets/emitir")
    public ResponseEntity<TicketDTO> emitirTicket() {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.emitirTicket());
    }
}
