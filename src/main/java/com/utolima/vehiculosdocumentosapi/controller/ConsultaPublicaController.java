package com.utolima.vehiculosdocumentosapi.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.utolima.vehiculosdocumentosapi.dto.ConteoPersonaPorTipoDTO;
import com.utolima.vehiculosdocumentosapi.dto.PersonaResponseDTO;
import com.utolima.vehiculosdocumentosapi.dto.VehiculoDetalleCompletoDTO;
import com.utolima.vehiculosdocumentosapi.dto.VehiculoResponseDTO;
import com.utolima.vehiculosdocumentosapi.mapper.PersonaMapper;
import com.utolima.vehiculosdocumentosapi.mapper.VehiculoMapper;
import com.utolima.vehiculosdocumentosapi.service.ConsultaPublicaService;

import lombok.RequiredArgsConstructor;

// todo lo que quede bajo /api/publico NO va a exigir token+APIKey cuando implementemos la seguridad
// por eso las separamos en su propio controlador desde ya, en vez de mezclarlas con VehiculoController/PersonaController
@RestController
@RequestMapping("/api/publico")
@RequiredArgsConstructor
public class ConsultaPublicaController {

    private final ConsultaPublicaService consultaPublicaService;

    // "Consultar todos los vehiculos que tengan documentos vencidos"
    @GetMapping("/vehiculos/documentos-vencidos")
    public ResponseEntity<List<VehiculoResponseDTO>> vehiculosConDocumentosVencidos() {
        return ResponseEntity.ok(VehiculoMapper.toResponseDTOList(
                consultaPublicaService.vehiculosConDocumentosVencidos()));
    }

    // "Consultar los vehiculos que tienen documentos por vencer con un tiempo que se especifique como parametro"
    @GetMapping("/vehiculos/documentos-por-vencer")
    public ResponseEntity<List<VehiculoResponseDTO>> vehiculosConDocumentosPorVencer(
            @RequestParam int dias) { // ?dias=30 en la URL, por ejemplo
        return ResponseEntity.ok(VehiculoMapper.toResponseDTOList(
                consultaPublicaService.vehiculosConDocumentosPorVencer(dias)));
    }

    // "Consultar todos los conductores que puedan operar"
    @GetMapping("/conductores-disponibles")
    public ResponseEntity<List<PersonaResponseDTO>> conductoresQuePuedenOperar() {
        List<PersonaResponseDTO> dtos = consultaPublicaService.conductoresQuePuedenOperar().stream()
                .map(p -> PersonaMapper.toResponseDTO(p, null, false)) // usuario siempre null: un CONDUCTOR nunca tiene Usuario
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // "Consultar un vehiculo por placa donde se relacione la informacion de los conductores asociados, y los documentos"
    @GetMapping("/vehiculos/placa/{placa}")
    public ResponseEntity<VehiculoDetalleCompletoDTO> vehiculoCompletoPorPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(consultaPublicaService.vehiculoCompletoPorPlaca(placa));
    }

    // "Consultar el total de las personas agrupadas por tipo"
    @GetMapping("/personas/conteo-por-tipo")
    public ResponseEntity<List<ConteoPersonaPorTipoDTO>> totalPersonasPorTipo() {
        return ResponseEntity.ok(consultaPublicaService.totalPersonasPorTipo());
    }
}