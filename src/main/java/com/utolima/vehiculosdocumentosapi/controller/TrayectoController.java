package com.utolima.vehiculosdocumentosapi.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utolima.vehiculosdocumentosapi.dto.TrayectoRequestDTO;
import com.utolima.vehiculosdocumentosapi.service.TrayectoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trayectos")
@RequiredArgsConstructor
public class TrayectoController {

    private final TrayectoService trayectoService;

    @PostMapping("/rutas")
    public ResponseEntity<Void> crearRuta(@RequestBody List<TrayectoRequestDTO> paradas) {
        trayectoService.crearRuta(paradas);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}