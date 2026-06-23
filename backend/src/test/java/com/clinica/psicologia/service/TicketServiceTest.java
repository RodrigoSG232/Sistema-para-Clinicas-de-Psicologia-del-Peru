package com.clinica.psicologia.service;

import com.clinica.psicologia.dto.TicketDTO;
import com.clinica.psicologia.entity.Ticket;
import com.clinica.psicologia.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketService service;

    @Test
    void emitirTicketCreaCorrelativoDelDiaEnEspera() {
        when(ticketRepository.getMaxCorrelativoPorFecha(any(LocalDate.class))).thenReturn(9);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(25);
            return ticket;
        });

        TicketDTO response = service.emitirTicket();

        assertThat(response.getId()).isEqualTo(25);
        assertThat(response.getNumero()).isEqualTo("A-010");
        assertThat(response.getEstado()).isEqualTo("ESPERA");

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getFecha()).isNotNull();
        assertThat(captor.getValue().getCreadoEn()).isNotNull();
    }

    @Test
    void llamarTicketBloqueaSiYaExisteTicketEnAtencion() {
        Ticket ticket = Ticket.builder()
                .id(10)
                .numero("A-001")
                .estado("ESPERA")
                .fecha(LocalDate.now())
                .creadoEn(LocalDateTime.now())
                .build();

        when(ticketRepository.findById(10)).thenReturn(Optional.of(ticket));
        when(ticketRepository.existsByFechaAndEstado(any(LocalDate.class), eq("EN_ATENCION"))).thenReturn(true);

        assertThatThrownBy(() -> service.llamarTicket(10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe");

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void finalizarTicketCambiaEstadoAFinalizado() {
        Ticket ticket = Ticket.builder()
                .id(10)
                .numero("A-001")
                .estado("EN_ATENCION")
                .fecha(LocalDate.now())
                .creadoEn(LocalDateTime.now())
                .build();

        when(ticketRepository.findById(10)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        TicketDTO response = service.finalizarTicket(10);

        assertThat(response.getEstado()).isEqualTo("FINALIZADO");
        assertThat(ticket.getEstado()).isEqualTo("FINALIZADO");
        verify(ticketRepository).save(ticket);
    }
}
