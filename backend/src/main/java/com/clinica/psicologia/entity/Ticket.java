package com.clinica.psicologia.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 10)
    private String numero;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "ESPERA";

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn; // ESPERA | EN_ATENCION | FINALIZADO
}