package com.utolima.vehiculosdocumentosapi.model;

import java.time.LocalDate;

import com.utolima.vehiculosdocumentosapi.model.enums.EstadoConductor;

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
@Table(name = "vehiculo_persona")
@Getter
@Setter
@NoArgsConstructor
public class VehiculoPersona {

    @EmbeddedId
    private VehiculoPersonaId id = new VehiculoPersonaId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idVehiculo")
    @JoinColumn(name = "id_vehiculo")
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idPersona")
    @JoinColumn(name = "id_persona")
    private Persona persona; // ojo: el PDF exige que aqui SOLO se asocien personas tipo CONDUCTOR -- lo validamos en el servicio

    @NotNull
    @Column(name = "fecha_asociacion", nullable = false)
    private LocalDate fechaAsociacion;

    @NotNull
    @Column(name = "estado_conductor", nullable = false, length = 2)
    private EstadoConductor estadoConductor;
}