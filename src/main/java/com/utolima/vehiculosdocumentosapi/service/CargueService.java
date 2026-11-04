package com.utolima.vehiculosdocumentosapi.service;

import org.springframework.web.multipart.MultipartFile;

import com.utolima.vehiculosdocumentosapi.dto.CargueRespuestaAsyncDTO;
import com.utolima.vehiculosdocumentosapi.dto.CargueResultadoDTO;

/**
 * Contrato del servicio encargado del cargue masivo y procesamiento asincrono
 * de archivos Excel (.xlsx) mediante RabbitMQ y procedimientos almacenados (JdbcTemplate).
 */
public interface CargueService {

    /**
     * Valida la estructura del archivo Excel, registra las paradas en trayecto_tmp bajo estado CARGADO,
     * confirma la transaccion de base de datos y publica un evento en RabbitMQ para su procesamiento
     * en segundo plano.
     *
     * @param archivo Archivo Excel (.xlsx) con la hoja "Trayectos" y los 7 encabezados requeridos.
     * @return DTO con HTTP 202 Accepted indicando que el cargue fue recibido con exito.
     */
    CargueRespuestaAsyncDTO recibirArchivo(MultipartFile archivo);

    /**
     * Ejecuta los procedimientos de validacion (sp_validacion) y traslado definitivo (sp_procesado).
     * Este metodo es invocado por el consumidor de RabbitMQ en segundo plano.
     *
     * @param idCargue Identificador del cargue a procesar.
     */
    void procesarCargueAsincrono(Long idCargue);

    /**
     * Consulta el estado actual y el conteo de paradas por estado (CARGADO, VALIDADO, PROCESADO, ERROR)
     * para un idCargue especifico.
     *
     * @param idCargue Identificador del cargue a consultar.
     * @return DTO con el resumen de conteos por cada uno de los 4 estados.
     */
    void marcarCargueFallido(Long idCargue, String motivo);
}
