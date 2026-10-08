package com.utolima.vehiculosdocumentosapi.controller;

import com.utolima.vehiculosdocumentosapi.dto.TrayectoRequestDTO;
import com.utolima.vehiculosdocumentosapi.service.TrayectoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trayectos")
public class TrayectoController {

    @Autowired
    private TrayectoService trayectoService;

    @PostMapping("/rutas")
    public ResponseEntity<Void> crearRuta(@RequestBody List<TrayectoRequestDTO> paradas) {
        trayectoService.crearRuta(paradas);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}