package com.clinica.psicologia.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "Especialidad")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Especialidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tarifa = new BigDecimal("80.00");

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
