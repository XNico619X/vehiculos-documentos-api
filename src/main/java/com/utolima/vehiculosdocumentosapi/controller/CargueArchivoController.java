package com.utolima.vehiculosdocumentosapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.utolima.vehiculosdocumentosapi.dto.CargueResultadoDTO;
import com.utolima.vehiculosdocumentosapi.service.CargueService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trayectos/cargues")
@RequiredArgsConstructor
public class CargueArchivoController {

    private final CargueService cargueService;

    @PostMapping
    public ResponseEntity<CargueResultadoDTO> cargarArchivo(@RequestParam("archivo") MultipartFile archivo) {
        CargueResultadoDTO resultado = cargueService.procesarArchivo(archivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }
}

