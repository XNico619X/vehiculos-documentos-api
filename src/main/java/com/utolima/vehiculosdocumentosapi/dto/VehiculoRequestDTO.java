package com.utolima.vehiculosdocumentosapi.dto;

import java.util.List;

import com.utolima.vehiculosdocumentosapi.model.enums.TipoCombustible;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoServicio;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoVehiculo;
import com.utolima.vehiculosdocumentosapi.validation.PlacaValida;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PlacaValida //la nueva anotacion para la validacion correcta de la placa
public class VehiculoRequestDTO {

    @NotNull
    private TipoVehiculo tipoVehiculo;

    @NotNull
    @Size(min = 6, max = 6)
    private String placa;

    @NotNull
    private TipoServicio tipoServicio;

    @NotNull
    private TipoCombustible tipoCombustible;

    @NotNull
    private Integer capacidadPasajeros;

    @NotNull
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    private String color;

    @NotNull
    private Integer modelo;

    @NotNull
    private String marca;

    @NotNull
    private String linea;

    @NotEmpty // valida que la lista no sea null NI vacia -- aqui es donde forzamos la regla "minimo 1 documento" a nivel de request
    @Valid // le dice a Spring: valida tambien cada objeto DENTRO de esta lista (sus @NotNull internos)
    private List<DocumentoAsociadoRequestDTO> documentos;
}