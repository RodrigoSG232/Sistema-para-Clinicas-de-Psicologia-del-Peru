package com.clinica.psicologia.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity @Table(name = "horariopsicologo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HorarioPsicologo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psicologo_id", nullable = false)
    private Psicologo psicologo;

    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana; // 1=Lun ... 6=Sab

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
