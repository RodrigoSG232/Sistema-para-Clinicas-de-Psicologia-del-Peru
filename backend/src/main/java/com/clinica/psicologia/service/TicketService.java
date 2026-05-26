package com.clinica.psicologia.service;

import com.clinica.psicologia.dto.TicketDTO;
import com.clinica.psicologia.entity.Ticket;
import com.clinica.psicologia.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private static final String ESTADO_ESPERA = "ESPERA";
    private static final String ESTADO_EN_ATENCION = "EN_ATENCION";
    private static final String ESTADO_FINALIZADO = "FINALIZADO";
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public List<TicketDTO> listarPorEstado(String estado) {
        LocalDate fechaOperativa = LocalDate.now(ZONA_LIMA);

        return ticketRepository.findByFechaAndEstadoOrderByCreadoEnAsc(fechaOperativa, estado).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketDTO> listarDelDia() {
        LocalDate fechaOperativa = LocalDate.now(ZONA_LIMA);

        return ticketRepository.findByFechaOrderByCreadoEnAsc(fechaOperativa).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public TicketDTO emitirTicket() {
        LocalDate fechaOperativa = LocalDate.now(ZONA_LIMA);
        LocalDateTime creadoEn = LocalDateTime.now(ZONA_LIMA);

        Integer maxCorrelativo = ticketRepository.getMaxCorrelativoPorFecha(fechaOperativa);
        int correlativo = (maxCorrelativo == null ? 0 : maxCorrelativo) + 1;

        Ticket ticket = Ticket.builder()
                .numero("A-" + String.format("%03d", correlativo))
                .fecha(fechaOperativa)
                .creadoEn(creadoEn)
                .estado(ESTADO_ESPERA)
                .build();

        return toDto(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketDTO llamarTicket(Integer id) {
        LocalDate fechaOperativa = LocalDate.now(ZONA_LIMA);
        Ticket ticket = obtenerTicket(id);

        if (!ESTADO_ESPERA.equals(ticket.getEstado())) {
            throw new IllegalStateException("Solo se puede llamar un ticket en espera");
        }

        if (ticketRepository.existsByFechaAndEstado(fechaOperativa, ESTADO_EN_ATENCION)) {
            throw new IllegalStateException("Ya existe un ticket en atencion");
        }

        ticket.setEstado(ESTADO_EN_ATENCION);
        return toDto(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketDTO finalizarTicket(Integer id) {
        Ticket ticket = obtenerTicket(id);

        if (!ESTADO_EN_ATENCION.equals(ticket.getEstado())) {
            throw new IllegalStateException("Solo se puede finalizar un ticket en atencion");
        }

        ticket.setEstado(ESTADO_FINALIZADO);
        return toDto(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public TicketDTO obtenerTicketActual() {
        LocalDate fechaOperativa = LocalDate.now(ZONA_LIMA);

        return ticketRepository.findFirstByFechaAndEstadoOrderByCreadoEnAsc(fechaOperativa, ESTADO_EN_ATENCION)
                .map(this::toDto)
                .orElse(null);
    }

    private Ticket obtenerTicket(Integer id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));
    }

    private TicketDTO toDto(Ticket ticket) {
        return new TicketDTO(
                ticket.getId(),
                ticket.getNumero(),
                ticket.getCreadoEn().toString(),
                ticket.getEstado());
    }
}
