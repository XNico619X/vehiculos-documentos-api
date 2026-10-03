package com.utolima.vehiculosdocumentosapi.dto;

import com.utolima.vehiculosdocumentosapi.model.enums.TipoPersona;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor // este es EL constructor que usa el "new ...ConteoPersonaPorTipoDTO(...)" del JPQL de arriba
public class ConteoPersonaPorTipoDTO {
    private TipoPersona tipoPersona;
    private Long total; // COUNT(p) en JPQL siempre devuelve Long, no int
}