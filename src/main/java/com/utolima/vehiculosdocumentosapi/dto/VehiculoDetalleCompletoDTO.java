package com.utolima.vehiculosdocumentosapi.dto;

import java.util.List;

import com.utolima.vehiculosdocumentosapi.model.enums.TipoCombustible;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoServicio;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoVehiculo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class VehiculoDetalleCompletoDTO {
    // mismos campos que VehiculoResponseDTO...
    private Long id;
    private TipoVehiculo tipoVehiculo;
    private String placa;
    private TipoServicio tipoServicio;
    private TipoCombustible tipoCombustible;
    private Integer capacidadPasajeros;
    private String color;
    private Integer modelo;
    private String marca;
    private String linea;
    private List<DocumentoAsociadoResponseDTO> documentos;
    // ...mas la lista de conductores, que es lo que pide esta consulta especifica los requisitos
    private List<ConductorAsociadoResponseDTO> conductores;
}