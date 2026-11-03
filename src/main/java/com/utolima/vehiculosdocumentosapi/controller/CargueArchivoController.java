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

/**
 * Controlador REST para el cargue masivo de trayectos a traves de archivos Excel (.xlsx).
 *
 * Expone el endpoint POST /api/trayectos/cargues protegido bajo la configuracion de seguridad
 * de la aplicacion (requiere JWT valido en el header Authorization y APIKey en el header X-API-Key).
 */
@RestController
@RequestMapping("/api/trayectos/cargues")
@RequiredArgsConstructor
public class CargueArchivoController {

    private final CargueService cargueService;

    /**
     * Recibe el archivo Excel cargado mediante multipart/form-data.
     *
     * ¿Por que responde con estado HTTP 201 (CREATED)?:
     * Porque la operacion crea un nuevo recurso de cargue en el sistema, identificado por su idCargue.
     * Aun si ciertas filas individuales no cumplen las reglas de negocio y quedan marcadas en estado ERROR,
     * la sesion de cargue fue creada y procesada con exito en la base de datos temporal, retornando el DTO
     * con el resumen de la operacion.
     *
     * @param archivo Archivo binario enviado bajo la clave "archivo".
     * @return ResponseEntity con HTTP 201 y el resumen del cargue en el cuerpo.
     */
    @PostMapping
    public ResponseEntity<CargueResultadoDTO> cargarArchivo(@RequestParam("archivo") MultipartFile archivo) {
        CargueResultadoDTO resultado = cargueService.procesarArchivo(archivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }
}
