package com.utolima.vehiculosdocumentosapi.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable // le dice a JPA: "esta clase no es una tabla propia, es un bloque de columnas que se incrusta dentro de otra entidad"
@Getter
@Setter
@NoArgsConstructor
public class VehiculoDocumentoId implements Serializable { // toda llave compuesta DEBE implementar Serializable, es obligatorio en JPA

    @Column(name = "id_vehiculo") // este nombre debe coincidir con la columna FK en la tabla vehiculo_documento
    private Long idVehiculo;

    @Column(name = "id_documento")
    private Long idDocumento;

    public VehiculoDocumentoId(Long idVehiculo, Long idDocumento) { // constructor util para armar la llave manualmente
        this.idVehiculo = idVehiculo;
        this.idDocumento = idDocumento;
    }

    @Override
    public boolean equals(Object o) { // OBLIGATORIO en llaves compuestas: JPA compara llaves con equals/hashCode, no con ==
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehiculoDocumentoId that = (VehiculoDocumentoId) o;
        return Objects.equals(idVehiculo, that.idVehiculo) && Objects.equals(idDocumento, that.idDocumento);
    }

    @Override
    public int hashCode() { // debe ser consistente con equals: si dos objetos son equals(), su hashCode debe ser igual
        return Objects.hash(idVehiculo, idDocumento);
    }
}