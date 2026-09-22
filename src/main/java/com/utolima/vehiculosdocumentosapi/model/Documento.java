package com.utolima.vehiculosdocumentosapi.model;

import com.utolima.vehiculosdocumentosapi.model.enums.AplicaVehiculo;
import com.utolima.vehiculosdocumentosapi.model.enums.Obligatoriedad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import jakarta.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonIgnore;


@Entity
@Table(name = "documentos") // nombre exacto de la tabla ya creada
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Long id;

    @NotNull
    @Column(name = "codigo_documento", nullable = false, length = 20) // codigo parametrizado, ej. "SOAT"
    private String codigoDocumento;

    @NotNull
    @Column(name = "nombre_documento", nullable = false, length = 100) // ej. "SOAT", "Tecnico Mecanica"
    private String nombreDocumento;

    @NotNull
    @Column(name = "tipo_vehiculo_aplica", nullable = false, length = 2)
    private AplicaVehiculo tipoVehiculoAplica; // A / M / AM

    @NotNull
    @Column(name = "obligatoriedad", nullable = false, length = 2)
    private Obligatoriedad obligatoriedad; // RA / RM / RR

    @Column(name = "descripcion", length = 255) // no lleva @NotNull: el PDF no la marca como obligatoria
    private String descripcion;
    
    @JsonIgnore
    @OneToMany(mappedBy = "documento", fetch = FetchType.LAZY)
    private List<VehiculoDocumento> vehiculosAsociados;
}