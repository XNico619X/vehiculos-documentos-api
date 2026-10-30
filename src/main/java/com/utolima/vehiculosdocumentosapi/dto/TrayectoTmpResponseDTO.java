package com.utolima.vehiculosdocumentosapi.dto;

// Una fila del cargue tal como la ve quien consulta: el dato del Excel SIN modificar (todo texto)
// más el estado y la observación que le puso la validación.
// Es un DTO (record) porque nunca devolvemos la entidad TrayectoTmp directamente.
public record TrayectoTmpResponseDTO(
        Long idTrayectoTmp,
        String idPersona,      // texto: así entró del Excel, aunque diga "abc"
        String idVehiculo,
        String codigoRuta,
        String ubicacion,
        String ordenParada,
        String latitud,
        String longitud,
        String loginUsuario,
        String estado,         // CARGADO, VALIDADO, PROCESADO o ERROR
        String observacion) {  // errores acumulados; null si no tiene
}