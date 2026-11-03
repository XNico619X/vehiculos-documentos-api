package com.utolima.vehiculosdocumentosapi.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.utolima.vehiculosdocumentosapi.dto.CargueResultadoDTO;
import com.utolima.vehiculosdocumentosapi.exception.ReglaNegocioException;
import com.utolima.vehiculosdocumentosapi.model.Usuario;
import com.utolima.vehiculosdocumentosapi.service.CargueService;

import lombok.RequiredArgsConstructor;

/**
 * Implementacion del servicio de cargue masivo de trayectos a traves de archivos Excel (.xlsx).
 *
 * Sigue una arquitectura basada en base de datos temporal (trayecto_tmp) y procedimientos
 * almacenados (sp_cargue, sp_validacion, sp_procesado) ejecutados exclusivamente mediante JdbcTemplate.
 */
@Service
@RequiredArgsConstructor
public class CargueServiceImpl implements CargueService {

    private static final String NOMBRE_HOJA = "Trayectos";
    private static final int MIN_FILAS_DATOS = 1;
    private static final int MAX_FILAS_DATOS = 500;

    /**
     * Lista inmutable con el orden exacto de los 7 encabezados requeridos en la Fila 1.
     */
    private static final List<String> ENCABEZADOS_ESPERADOS = List.of(
            "id_persona",
            "id_vehiculo",
            "codigo_ruta",
            "ubicacion",
            "orden_parada",
            "latitud",
            "longitud"
    );

    private final JdbcTemplate jdbcTemplate;

    /**
     * Procesa el archivo Excel completo dentro de una unica transaccion de base de datos.
     *
     * ¿Por que una sola transaccion (@Transactional)?:
     * Garantiza atomicidad total del cargue. El ciclo completo (insercion de filas en trayecto_tmp,
     * ejecucion de sp_validacion, ejecucion de sp_procesado y consulta del resumen) ocurre bajo
     * una misma unidad de trabajo. Si ocurre una caida de red, fallo de servidor o error inesperado de base de datos,
     * se efectua un rollback automatico, evitando que queden registros huerfanos o datos a medio procesar.
     *
     * @param archivo Archivo Excel (.xlsx) enviado en la peticion.
     * @return DTO con el identificador del cargue, total de filas y resumen de conteos por estado.
     */
    @Override
    @Transactional
    public CargueResultadoDTO procesarArchivo(MultipartFile archivo) {
        // 1. Validar Archivo basico
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("El archivo no puede ser nulo ni estar vacío.");
        }

        String nombreArchivo = archivo.getOriginalFilename();
        if (nombreArchivo == null || !nombreArchivo.toLowerCase().endsWith(".xlsx")) {
            throw new ReglaNegocioException("El archivo debe tener extensión .xlsx.");
        }

        // Se inicializa DataFormatter con Locale.US para asegurar que el formato de numeros
        // y fechas no dependa de la configuracion regional ni del idioma del sistema operativo servidor.
        DataFormatter dataFormatter = new DataFormatter(Locale.US);
        List<String[]> filasDatos = new ArrayList<>();

        // Apertura del libro de trabajo con Apache POI
        try (Workbook workbook = new XSSFWorkbook(archivo.getInputStream())) {
            Sheet hoja = workbook.getSheet(NOMBRE_HOJA);
            if (hoja == null) {
                throw new ReglaNegocioException("El archivo no contiene la hoja llamada 'Trayectos'.");
            }

            // Validacion de la fila 1 de encabezados
            Row filaEncabezado = hoja.getRow(0);
            validarEncabezados(filaEncabezado, dataFormatter);

            // Lectura de las filas de datos (a partir de la fila 2, indice 1)
            int ultimaFila = hoja.getLastRowNum();
            for (int i = 1; i <= ultimaFila; i++) {
                Row fila = hoja.getRow(i);
                if (esFilaVacia(fila, dataFormatter)) {
                    continue; // Se ignoran filas totalmente vacias
                }

                // ¿Por que NO se hace trim() de las celdas de datos?:
                // Las celdas se extraen como texto exacto mediante dataFormatter.formatCellValue(cell).
                // No se recortan espacios ni se alteran ceros a la izquierda para que entren en trayecto_tmp
                // tal como vinieron en el Excel. De esta forma, sp_validacion puede detectar si el usuario
                // envio espacios indebidos, caracteres invalidos o ceros a la izquierda sin enmascarar errores.
                String[] valores = new String[ENCABEZADOS_ESPERADOS.size()];
                for (int col = 0; col < ENCABEZADOS_ESPERADOS.size(); col++) {
                    Cell cell = fila.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    valores[col] = (cell == null) ? null : dataFormatter.formatCellValue(cell);
                }
                filasDatos.add(valores);
            }
        } catch (ReglaNegocioException e) {
            // Los errores propios de negocio (falta hoja, encabezado invalido, etc.) se propagan tal cual a HTTP 400.
            throw e;
        } catch (IOException | RuntimeException e) {
            // POI lanza IOException o RuntimeException no comprobadas (por ejemplo NotOfficeXmlFileException si se
            // sube un archivo PDF o binario corrupto renombrado a .xlsx). Se captura para evitar un HTTP 500
            // y responder un HTTP 400 claro para el cliente.
            throw new ReglaNegocioException("El archivo no es un Excel .xlsx válido.");
        }

        // Validacion de rango de filas de datos (entre 1 y 500)
        if (filasDatos.size() < MIN_FILAS_DATOS || filasDatos.size() > MAX_FILAS_DATOS) {
            throw new ReglaNegocioException(
                    "El archivo debe contener entre " + MIN_FILAS_DATOS + " y " + MAX_FILAS_DATOS + " filas de datos.");
        }

        // 2. Preparar ID y Validar Concurrencia
        // ¿Por que se rechaza un segundo cargue en el mismo minuto?:
        // idCargue se calcula con el patron yyyyMMddHHmm. Si se permitiesen dos cargues dentro del mismo
        // minuto cronologico, ambos compartirian el mismo identificador, mezclando paradas de archivos distintos
        // en trayecto_tmp y corrompiendo la auditoria y el procesamiento por lotes.
        Long idCargue = Long.parseLong(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")));
        Long conteoExistente = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trayecto_tmp WHERE id_cargue = ?",
                Long.class,
                idCargue
        );
        if (conteoExistente != null && conteoExistente > 0) {
            throw new ReglaNegocioException("Ya hay un cargue en este minuto; espera un minuto e inténtalo de nuevo.");
        }

        // 3. Obtener Login del usuario autenticado (se toma del contexto de seguridad, nunca del body)
        String login = obtenerLoginAutenticado();

        // 4. Procesar Filas (Paso 1: guardar tal cual en trayecto_tmp en estado CARGADO)
        for (String[] fila : filasDatos) {
            jdbcTemplate.update(
                    "CALL sp_cargue(?,?,?,?,?,?,?,?,?)",
                    idCargue,
                    fila[0], // idPersona
                    fila[1], // idVehiculo
                    fila[2], // codigoRuta
                    fila[3], // ubicacion
                    fila[4], // ordenParada
                    fila[5], // latitud
                    fila[6], // longitud
                    login
            );
        }

        // 5. Procedimientos Finales
        // sp_validacion valida formato, rutas (2 a 7 paradas, mismo conductor/vehiculo, orden 0..n-1),
        // conductor PO, vehiculo habilitado y unicidad de ruta en la base de datos real.
        jdbcTemplate.update("CALL sp_validacion(?)", idCargue);

        // sp_procesado traslada a la tabla trayecto unicamente las rutas completas en estado VALIDADO.
        jdbcTemplate.update("CALL sp_procesado(?)", idCargue);

        // ¿Por que las filas en ERROR no lanzan una excepcion?:
        // Si una o varias filas quedan marcadas con estado ERROR, se trata de rechazos de reglas de negocio
        // a nivel de fila/ruta. La ejecucion del servicio fue exitosa y se retorna HTTP 201 con el mapa de resumen
        // para que el cliente consulte cuantas filas fueron procesadas y cuantas tuvieron errores.
        Map<String, Long> resumen = new LinkedHashMap<>();
        resumen.put("CARGADO", 0L);
        resumen.put("VALIDADO", 0L);
        resumen.put("PROCESADO", 0L);
        resumen.put("ERROR", 0L);

        jdbcTemplate.query(
                "SELECT estado, COUNT(*) FROM trayecto_tmp WHERE id_cargue = ? GROUP BY estado",
                rs -> {
                    String estado = rs.getString(1);
                    long cantidad = rs.getLong(2);
                    if (estado != null) {
                        resumen.put(estado, cantidad);
                    }
                },
                idCargue
        );

        return new CargueResultadoDTO(idCargue, filasDatos.size(), resumen);
    }

    /**
     * Valida que la primera fila del libro contenga exactamente los 7 encabezados esperados en orden.
     * En este caso SI se aplica .trim() para evitar rechazar archivos donde el usuario coloco espacios
     * accidentales en los titulos de las columnas.
     */
    private void validarEncabezados(Row filaEncabezado, DataFormatter dataFormatter) {
        if (filaEncabezado == null) {
            throw new ReglaNegocioException("La fila 1 de encabezados no existe en la hoja 'Trayectos'.");
        }

        short ultimaCelda = filaEncabezado.getLastCellNum();
        if (ultimaCelda > ENCABEZADOS_ESPERADOS.size()) {
            for (int col = ENCABEZADOS_ESPERADOS.size(); col < ultimaCelda; col++) {
                Cell extra = filaEncabezado.getCell(col);
                if (extra != null && !dataFormatter.formatCellValue(extra).trim().isBlank()) {
                    throw new ReglaNegocioException("La fila 1 debe tener exactamente los 7 encabezados requeridos.");
                }
            }
        }

        for (int col = 0; col < ENCABEZADOS_ESPERADOS.size(); col++) {
            Cell cell = filaEncabezado.getCell(col);
            String valor = (cell == null) ? "" : dataFormatter.formatCellValue(cell).trim();
            if (!ENCABEZADOS_ESPERADOS.get(col).equals(valor)) {
                throw new ReglaNegocioException(
                        "Encabezado inválido en la columna " + (col + 1) + ": se esperaba '"
                                + ENCABEZADOS_ESPERADOS.get(col) + "'.");
            }
        }
    }

    /**
     * Verifica si una fila carece de contenido en todas sus celdas.
     */
    private boolean esFilaVacia(Row fila, DataFormatter dataFormatter) {
        if (fila == null) {
            return true;
        }
        short ultimaCelda = fila.getLastCellNum();
        if (ultimaCelda <= 0) {
            return true;
        }
        for (int col = 0; col < ultimaCelda; col++) {
            Cell cell = fila.getCell(col);
            if (cell != null && !dataFormatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extrae el login del usuario autenticado en la sesion a partir del SecurityContextHolder.
     */
    private String obtenerLoginAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Usuario usuario && usuario.getId() != null) {
                return usuario.getId().getLogin();
            }
            if (auth.getName() != null && !auth.getName().isBlank()) {
                return auth.getName();
            }
        }
        throw new ReglaNegocioException("No se pudo identificar al usuario autenticado.");
    }
}
