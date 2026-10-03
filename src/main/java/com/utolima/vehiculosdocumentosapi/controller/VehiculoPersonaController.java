package com.utolima.vehiculosdocumentosapi.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utolima.vehiculosdocumentosapi.dto.CambioEstadoConductorDTO;
import com.utolima.vehiculosdocumentosapi.dto.VehiculoPersonaRequestDTO;
import com.utolima.vehiculosdocumentosapi.model.VehiculoPersona;
import com.utolima.vehiculosdocumentosapi.service.VehiculoPersonaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/vehiculos") // comparte el mismo prefijo base que VehiculoController -- son sub-rutas de vehiculo
@RequiredArgsConstructor
public class VehiculoPersonaController {

    private final VehiculoPersonaService vehiculoPersonaService;

    // "Realizar un servicio que permita asociar los vehiculos que puede operar un conductor especifico"
    @PostMapping("/{idVehiculo}/conductores")
    public ResponseEntity<VehiculoPersona> asociarConductor(
            @PathVariable Long idVehiculo,
            @Valid @RequestBody VehiculoPersonaRequestDTO dto) {

        VehiculoPersona creado = vehiculoPersonaService.asociarConductor(idVehiculo, dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    // "Realizar un servicio que permita cambiar el estado del conductor en relacion con el vehiculo"
    @PutMapping("/{idVehiculo}/conductores/{idPersona}/estado")
    public ResponseEntity<VehiculoPersona> cambiarEstadoConductor(
            @PathVariable Long idVehiculo,
            @PathVariable Long idPersona,
            @Valid @RequestBody CambioEstadoConductorDTO dto) {

        VehiculoPersona actualizado = vehiculoPersonaService.cambiarEstado(idVehiculo, idPersona, dto);
        return ResponseEntity.ok(actualizado);
    }

    // Extra util para probar: listar los conductores de un vehiculo especifico
    @org.springframework.web.bind.annotation.GetMapping("/{idVehiculo}/conductores")
    public ResponseEntity<List<VehiculoPersona>> listarConductoresDeVehiculo(@PathVariable Long idVehiculo) {
        return ResponseEntity.ok(vehiculoPersonaService.listarConductoresDeVehiculo(idVehiculo));
    }
}