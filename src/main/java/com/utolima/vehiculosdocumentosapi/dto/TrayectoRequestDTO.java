package com.utolima.vehiculosdocumentosapi.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class TrayectoRequestDTO {
    private Long idPersona;
    private Long idVehiculo;
    private String codigoRuta;
    private String ubicacion;
    private Integer ordenParada;
    private BigDecimal latitud; // Puede ser null al inicio, se llena con el API de Google Maps después
    private BigDecimal longitud; // Puede ser null al inicio
    private String loginUsuario;
}