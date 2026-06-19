package com.clinica.psicologia.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity @Table(name = "psicologo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Psicologo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 50)
    private String colegiatura;

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String email;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @OneToMany(mappedBy = "psicologo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HorarioPsicologo> horarios;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}
