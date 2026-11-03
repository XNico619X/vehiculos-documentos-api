package com.utolima.vehiculosdocumentosapi.service;

import org.springframework.web.multipart.MultipartFile;

import com.utolima.vehiculosdocumentosapi.dto.CargueResultadoDTO;

/**
 * Contrato del servicio encargado del procesamiento y validacion de archivos Excel (.xlsx)
 * para el cargue masivo de trayectos.
 *
 * Desacopla la logica de negocio y acceso a datos por JdbcTemplate de los controladores web.
 */
public interface CargueService {

    /**
     * Valida la estructura del archivo Excel, registra las paradas en la tabla temporal trayecto_tmp,
     * invoca los procedimientos de validacion y traslado definitivo, y genera el resumen por estados.
     *
     * @param archivo Archivo Excel (.xlsx) multipart con la hoja "Trayectos" y los encabezados requeridos.
     * @return DTO con el id del cargue, total de filas de datos y resumen de estados.
     * @throws com.utolima.vehiculosdocumentosapi.exception.ReglaNegocioException Si el archivo es invalido,
     *         no es un Excel valido, no cumple la estructura o ya existe un cargue en el mismo minuto.
     */
    CargueResultadoDTO procesarArchivo(MultipartFile archivo);
}
