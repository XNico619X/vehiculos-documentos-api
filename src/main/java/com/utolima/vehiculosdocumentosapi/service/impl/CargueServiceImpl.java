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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.utolima.vehiculosdocumentosapi.config.RabbitMQConfig;
import com.utolima.vehiculosdocumentosapi.dto.CargueMensajeDTO;
import com.utolima.vehiculosdocumentosapi.dto.CargueRespuestaAsyncDTO;
import com.utolima.vehiculosdocumentosapi.dto.CargueResultadoDTO;
import com.utolima.vehiculosdocumentosapi.exception.RecursoNoEncontradoException;
import com.utolima.vehiculosdocumentosapi.exception.ReglaNegocioException;
import com.utolima.vehiculosdocumentosapi.model.Usuario;
import com.utolima.vehiculosdocumentosapi.service.CargueService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementacion del servicio de cargue masivo asincrono mediante RabbitMQ y procedimientos
 * almacenados ejecutados con JdbcTemplate.
 *
 * Flujo desacoplado:
 * 1. recibirArchivo(): Valida el Excel, inserta las filas en trayecto_tmp (estado CARGADO)
 *    dentro de una transaccion y, UNICAMENTE tras confirmar el commit, publica un evento en RabbitMQ.
 * 2. procesarCargueAsincrono(): El consumidor de RabbitMQ ejecuta sp_validacion y sp_procesado
 *    en segundo plano sin bloquear la solicitud HTTP del usuario.
 * 3. consultarProgreso(): Endpoint de sondeo para consultar el resumen por estados para un idCargue.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CargueServiceImpl implements CargueService {

    private static final String NOMBRE_HOJA = "Trayectos";
    private static final int MIN_FILAS_DATOS = 1;
    private static final int MAX_FILAS_DATOS = 500;

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
    private final RabbitTemplate rabbitTemplate;

    /**
     * Paso 1: Recibe el archivo, valida su contenido, inserta las paradas en estado CARGADO
     * y programa la publicacion a RabbitMQ tras confirmar el commit en la base de datos.
     */
    @Override
    @Transactional
    public CargueRespuestaAsyncDTO recibirArchivo(MultipartFile archivo) {
        // 1. Validar Archivo
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("El archivo no puede ser nulo ni estar vacío.");
        }

        String nombreArchivo = archivo.getOriginalFilename();
        if (nombreArchivo == null || !nombreArchivo.toLowerCase().endsWith(".xlsx")) {
            throw new ReglaNegocioException("El archivo debe tener extensión .xlsx.");
        }

        DataFormatter dataFormatter = new DataFormatter(Locale.US);
        List<String[]> filasDatos = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(archivo.getInputStream())) {
            Sheet hoja = workbook.getSheet(NOMBRE_HOJA);
            if (hoja == null) {
                throw new ReglaNegocioException("El archivo no contiene la hoja llamada 'Trayectos'.");
            }

            Row filaEncabezado = hoja.getRow(0);
            validarEncabezados(filaEncabezado, dataFormatter);

            int ultimaFila = hoja.getLastRowNum();
            for (int i = 1; i <= ultimaFila; i++) {
                Row fila = hoja.getRow(i);
                if (esFilaVacia(fila, dataFormatter)) {
                    continue;
                }

                // Las celdas se extraen como texto exacto (sin alterar ni recortar espacios)
                String[] valores = new String[ENCABEZADOS_ESPERADOS.size()];
                for (int col = 0; col < ENCABEZADOS_ESPERADOS.size(); col++) {
                    Cell cell = fila.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    valores[col] = (cell == null) ? null : dataFormatter.formatCellValue(cell);
                }
                filasDatos.add(valores);
            }
        } catch (ReglaNegocioException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new ReglaNegocioException("El archivo no es un Excel .xlsx válido.");
        }

        if (filasDatos.size() < MIN_FILAS_DATOS || filasDatos.size() > MAX_FILAS_DATOS) {
            throw new ReglaNegocioException(
                    "El archivo debe contener entre " + MIN_FILAS_DATOS + " y " + MAX_FILAS_DATOS + " filas de datos.");
        }

        // 2. Preparar ID y Validar Concurrencia por minuto
        Long idCargue = Long.parseLong(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")));
        Long conteoExistente = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trayecto_tmp WHERE id_cargue = ?",
                Long.class,
                idCargue
        );
        if (conteoExistente != null && conteoExistente > 0) {
            throw new ReglaNegocioException("Ya hay un cargue en este minuto; espera un minuto e inténtalo de nuevo.");
        }

        // 3. Obtener Login del usuario autenticado
        String login = obtenerLoginAutenticado();

        // 4. Guardar filas en trayecto_tmp en estado CARGADO (sp_cargue)
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

        // 5. Publicar en RabbitMQ DESPUES de confirmar la transaccion (evita lecturas sucias en el consumidor)
        CargueMensajeDTO mensaje = new CargueMensajeDTO(idCargue, filasDatos.size(), login);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("Transaccion confirmada. Publicando cargue {} a RabbitMQ...", idCargue);
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_CARGUE,
                        RabbitMQConfig.ROUTING_KEY_CARGUE,
                        mensaje
                );
            }
        });

        return new CargueRespuestaAsyncDTO(
                idCargue,
                filasDatos.size(),
                "RECIBIDO",
                "El archivo fue recibido exitosamente y su validación está encolada para procesamiento en segundo plano."
        );
    }

    /**
     * Paso 2: Ejecutado de manera asincrona por el consumidor de RabbitMQ.
     * Invoca los procedimientos almacenados de validacion y procesado definitivo.
     */
    @Override
    @Transactional
    public void procesarCargueAsincrono(Long idCargue) {
        log.info("Iniciando validacion y procesado asincrono para el cargue: {}", idCargue);
        jdbcTemplate.update("CALL sp_validacion(?)", idCargue);
        jdbcTemplate.update("CALL sp_procesado(?)", idCargue);
        log.info("Cargue {} procesado exitosamente por RabbitMQ.", idCargue);
    }

    /**
     * Paso 3: Permite consultar el estado actual del cargue por su idCargue.
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
 // Se usa cuando RabbitMQ agoto los reintentos: deja el fallo VISIBLE para quien consulta el cargue.
 // Solo toca filas que no terminaron (CARGADO o VALIDADO); lo ya procesado no se modifica.
 @Override
 @Transactional
 public void marcarCargueFallido(Long idCargue, String motivo) {
     jdbcTemplate.update(
             "UPDATE trayecto_tmp SET estado = 'ERROR', observacion = CONCAT_WS('; ', observacion, ?) "
                     + "WHERE id_cargue = ? AND estado IN ('CARGADO', 'VALIDADO')",
             motivo, idCargue);
 }
}
