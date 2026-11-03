package com.utolima.vehiculosdocumentosapi.dto;

import java.util.Map;

/**
 * DTO (Java Record) que transporta el resultado final del procesamiento de un archivo Excel de trayectos.
 *
 * Se utiliza un Record por ser inmutable y representar un contenedor de datos transparente para la API.
 *
 * @param idCargue   Identificador único del proceso de cargue generado con el patrón yyyyMMddHHmm.
 *                   Permite auditar y agrupar en trayecto_tmp todas las filas correspondientes a esta ejecución.
 * @param totalFilas Total de filas con contenido leídas del archivo Excel (sin contar la fila de encabezados
 *                   ni las filas completamente en blanco).
 * @param resumen    Mapa ordenado (LinkedHashMap) con el conteo de filas en cada uno de los cuatro estados:
 *                   "CARGADO", "VALIDADO", "PROCESADO", "ERROR".
 *
 *                   ¿Por qué las filas en estado ERROR no lanzan una excepción?:
 *                   Las filas marcadas en ERROR por el procedimiento sp_validacion representan inconsistencias
 *                   a nivel de datos de negocio en registros individuales o en la conformación de la ruta, NO una
 *                   falla en el servicio de cargue. El archivo fue recibido y procesado correctamente; reportar
 *                   el conteo en el resumen con estado HTTP 201 (Created) permite al cliente conocer el balance
 *                   de paradas exitosas y erróneas, y consultar el detalle de las observaciones en la tabla temporal
 *                   sin forzar un rollback general del proceso de auditoría del cargue.
 */
public record CargueResultadoDTO(
        Long idCargue,
        int totalFilas,
        Map<String, Long> resumen
) {
}
