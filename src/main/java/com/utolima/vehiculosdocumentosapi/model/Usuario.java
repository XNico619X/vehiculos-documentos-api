package com.utolima.vehiculosdocumentosapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @EmbeddedId
    private UsuarioId id = new UsuarioId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idPersona")
    @JoinColumn(name = "id_persona")
    private Persona persona;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "apikey", nullable = false, unique = true)
    private String apikey;
}
