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
public class VehiculoPersonaId implements Serializable {

    @Column(name = "id_vehiculo")
    private Long idVehiculo;

    @Column(name = "id_persona")
    private Long idPersona;

    public VehiculoPersonaId(Long idVehiculo, Long idPersona) {
        this.idVehiculo = idVehiculo;
        this.idPersona = idPersona;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehiculoPersonaId that = (VehiculoPersonaId) o;
        return Objects.equals(idVehiculo, that.idVehiculo) && Objects.equals(idPersona, that.idPersona);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idVehiculo, idPersona);
    }
}