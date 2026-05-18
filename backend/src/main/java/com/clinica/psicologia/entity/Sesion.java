package com.clinica.psicologia.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "Sesion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sesion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proceso_terapeutico_id", nullable = false)
    private ProcesoTerapeutico procesoTerapeutico;

    @Column(name = "fase_sesion", nullable = false)
    private Integer faseSesion;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String evolucion;

    @Column(name = "indicaciones_paciente", columnDefinition = "NVARCHAR(MAX)")
    private String indicacionesPaciente;

    @Column(name = "fecha_registro", nullable = false)
    @Builder.Default
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor;
}
