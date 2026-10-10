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
    private BigDecimal latitud;   // opcional: si no viene, la tarea de Google Maps la completa
    private BigDecimal longitud;  // opcional
}