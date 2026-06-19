package com.clinica.psicologia.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "comprobantepago")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComprobantePago {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "numero_comprobante", nullable = false, unique = true, length = 20)
    private String numeroComprobante;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String tipo = "BOLETA"; // BOLETA | FACTURA

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deuda_id", nullable = false)
    private Deuda deuda;

    // EFECTIVO | TARJETA | YAPE_PLIN
    @Column(name = "medio_pago", nullable = false, length = 30)
    private String medioPago;

    @Column(name = "monto_pagado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "fecha_pago", nullable = false)
    @Builder.Default
    private LocalDateTime fechaPago = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cajero_id")
    private Usuario cajero;
}
