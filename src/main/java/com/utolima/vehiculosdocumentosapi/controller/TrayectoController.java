package com.utolima.vehiculosdocumentosapi.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utolima.vehiculosdocumentosapi.dto.TrayectoRequestDTO;
import com.utolima.vehiculosdocumentosapi.dto.TrayectoResponseDTO;
import com.utolima.vehiculosdocumentosapi.service.TrayectoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trayectos") // prefijo común de todos los servicios de trayectos
@RequiredArgsConstructor          // genera el constructor que inyecta trayectoService
public class TrayectoController {

    private final TrayectoService trayectoService;

    // Ya existía: crea una ruta completa (lista de paradas)
    @PostMapping("/rutas")
    public ResponseEntity<Void> crearRuta(@RequestBody List<TrayectoRequestDTO> paradas) {
        trayectoService.crearRuta(paradas);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // CONSULTA 1 (protegida): como la ruta NO empieza por /api/publico/, exige token como los demás servicios.
    // @PathVariable toma {codigoRuta} de la URL y lo entrega como parámetro del método.
    @GetMapping("/rutas/{codigoRuta}")
    public ResponseEntity<List<TrayectoResponseDTO>> consultarRuta(@PathVariable String codigoRuta) {
        return ResponseEntity.ok(trayectoService.consultarRutaPorCodigo(codigoRuta));
    }
}