package com.utolima.vehiculosdocumentosapi.model;

import java.time.LocalDate;

import com.utolima.vehiculosdocumentosapi.model.enums.EstadoDocumento;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehiculo_documento")
@Getter
@Setter
@NoArgsConstructor
public class VehiculoDocumento {

    @EmbeddedId // la llave primaria de esta entidad no es un campo simple, es el objeto VehiculoDocumentoId completo
    private VehiculoDocumentoId id = new VehiculoDocumentoId(); // se inicializa vacio para poder ir llenandolo

    @ManyToOne(fetch = FetchType.LAZY) // muchos VehiculoDocumento pueden apuntar a un mismo Vehiculo
    @MapsId("idVehiculo") // le dice a JPA: "la parte idVehiculo de mi llave compuesta viene de esta relacion"
    @JoinColumn(name = "id_vehiculo") // la columna FK real en la tabla
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY) // muchos VehiculoDocumento pueden apuntar a un mismo Documento
    @MapsId("idDocumento") // igual, pero para la otra mitad de la llave compuesta
    @JoinColumn(name = "id_documento")
    private Documento documento;

    @NotNull
    @Column(name = "fecha_expedicion", nullable = false)
    private LocalDate fechaExpedicion; // LocalDate mapea directo a una columna DATE de SQL

    @NotNull
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @NotNull
    @Column(name = "estado_documento", nullable = false, length = 20)
    private EstadoDocumento estadoDocumento;
}