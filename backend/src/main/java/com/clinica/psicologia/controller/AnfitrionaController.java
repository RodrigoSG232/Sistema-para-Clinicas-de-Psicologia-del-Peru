package com.clinica.psicologia.controller;

import com.clinica.psicologia.dto.TicketDTO;
import com.clinica.psicologia.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/anfitriona")
@RequiredArgsConstructor
public class AnfitrionaController {

    private final TicketService ticketService;

    @PostMapping("/tickets/emitir")
    public ResponseEntity<TicketDTO> emitirTicket() {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.emitirTicket());
    }

    @GetMapping("/tickets/hoy")
    public ResponseEntity<List<TicketDTO>> listarTicketsHoy() {
        return ResponseEntity.ok(ticketService.listarDelDia());
    }
}
