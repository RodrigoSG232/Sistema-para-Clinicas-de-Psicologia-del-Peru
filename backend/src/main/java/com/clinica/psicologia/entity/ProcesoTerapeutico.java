package com.clinica.psicologia.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "procesoterapeutico")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcesoTerapeutico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psicologo_id", nullable = false)
    private Psicologo psicologo;

    // 1=Evaluación | 2=Explicación Hipótesis | 3=Tratamiento | 4=Seguimiento
    @Column(name = "fase_actual", nullable = false)
    @Builder.Default
    private Integer faseActual = 1;

    @Column(name = "fecha_inicio", nullable = false)
    @Builder.Default
    private LocalDate fechaInicio = LocalDate.now();

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
