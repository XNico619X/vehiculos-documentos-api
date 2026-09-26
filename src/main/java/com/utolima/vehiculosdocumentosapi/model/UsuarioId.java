package com.utolima.vehiculosdocumentosapi.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class UsuarioId implements Serializable {

    @Column(name = "id_persona")
    private Long idPersona;

    @Column(name = "login")
    private String login;

    public UsuarioId(Long idPersona, String login) {
        this.idPersona = idPersona;
        this.login = login;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioId that = (UsuarioId) o;
        return Objects.equals(idPersona, that.idPersona) && Objects.equals(login, that.login);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPersona, login);
    }
}