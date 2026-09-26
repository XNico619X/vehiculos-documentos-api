package com.utolima.vehiculosdocumentosapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "persona")
@Getter
@Setter
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_persona")
    private Long idPersona;

    @Column(name = "identificacion", nullable = false, unique = true)
    private String identificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_identificacion", nullable = false)
    private TipoIdentificacion tipoIdentificacion;

    @Column(name = "nombres", nullable = false)
    private String nombres;

    @Column(name = "apellidos", nullable = false)
    private String apellidos;

    @Column(name = "correo_electronico", nullable = false)
    private String correoElectronico;

    @Column(name = "tipo_persona", nullable = false)
    private TipoPersona tipoPersona;
}