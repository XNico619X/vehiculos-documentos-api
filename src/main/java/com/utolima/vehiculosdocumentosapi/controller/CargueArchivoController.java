package com.utolima.vehiculosdocumentosapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.utolima.vehiculosdocumentosapi.dto.CargueRespuestaAsyncDTO;
import com.utolima.vehiculosdocumentosapi.service.CargueService;

import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para el cargue masivo asincrono de trayectos a traves de archivos Excel (.xlsx).
 *
 * Expone:
 * 1. POST /api/trayectos/cargues: Recibe el archivo, valida su estructura, almacena en trayecto_tmp
 *    y encola el procesamiento en RabbitMQ, respondiendo inmediatamente con HTTP 202 (ACCEPTED).
 * 2. GET /api/trayectos/cargues/{idCargue}: Consulta el progreso y resumen de estados para el cargue indicado.
 */
@RestController
@RequestMapping("/api/trayectos/cargues")
@RequiredArgsConstructor
public class CargueArchivoController {

    private final CargueService cargueService;

    /**
     * Recibe el archivo Excel cargado mediante multipart/form-data.
     *
     * Responde con estado HTTP 202 (ACCEPTED) indicando que la solicitud fue aceptada
     * y su procesamiento pesado (validacion de negocio y traslado definitivo) se ejecuta
     * en segundo plano mediante RabbitMQ.
     *
     * @param archivo Archivo binario enviado bajo la clave "archivo".
     * @return ResponseEntity con HTTP 202 y el DTO de respuesta asincrona.
     */
    @PostMapping
    public ResponseEntity<CargueRespuestaAsyncDTO> cargarArchivo(@RequestParam("archivo") MultipartFile archivo) {
        CargueRespuestaAsyncDTO respuesta = cargueService.recibirArchivo(archivo);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respuesta);
    }

    /**
     * Consulta el estado de avance y el resumen de un cargue por su identificador.
     *
     * @param idCargue Identificador del cargue (patron yyyyMMddHHmm).
     * @return Resumen con los conteos en CARGADO, VALIDADO, PROCESADO y ERROR.
     */
 
}
