package com.utolima.vehiculosdocumentosapi.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    @Override
    @Transactional
    public CargueResultadoDTO procesarArchivo(MultipartFile archivo) {
        // 1. Validar Archivo
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("El archivo no puede ser nulo ni estar vacío.");
        }

        String nombreArchivo = archivo.getOriginalFilename();
        if (nombreArchivo == null || !nombreArchivo.toLowerCase().endsWith(".xlsx")) {
            throw new ReglaNegocioException("El archivo debe tener extensión .xlsx.");
        }

        DataFormatter dataFormatter = new DataFormatter();
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

                String[] valores = new String[ENCABEZADOS_ESPERADOS.size()];
                for (int col = 0; col < ENCABEZADOS_ESPERADOS.size(); col++) {
                    Cell cell = fila.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    valores[col] = (cell == null) ? null : dataFormatter.formatCellValue(cell);
                }
                filasDatos.add(valores);
            }
        } catch (IOException e) {
            throw new ReglaNegocioException("No fue posible leer el archivo Excel (.xlsx).");
        }

        if (filasDatos.size() < MIN_FILAS_DATOS || filasDatos.size() > MAX_FILAS_DATOS) {
            throw new ReglaNegocioException(
                    "El archivo debe contener entre " + MIN_FILAS_DATOS + " y " + MAX_FILAS_DATOS + " filas de datos.");
        }

        // 2. Preparar ID y Validar Concurrencia
        Long idCargue = Long.parseLong(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")));
        Long conteoExistente = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trayecto_tmp WHERE id_cargue = ?",
                Long.class,
                idCargue
        );
        if (conteoExistente != null && conteoExistente > 0) {
            throw new ReglaNegocioException("Ya hay un cargue en este minuto; espera un minuto e inténtalo de nuevo.");
        }

        // 3. Obtener Login
        String login = obtenerLoginAutenticado();

        // 4. Procesar Filas
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

        // 5. Procedimientos Finales y Resumen
        jdbcTemplate.update("CALL sp_validacion(?)", idCargue);
        jdbcTemplate.update("CALL sp_procesado(?)", idCargue);

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

    private void validarEncabezados(Row filaEncabezado, DataFormatter dataFormatter) {
        if (filaEncabezado == null) {
            throw new ReglaNegocioException("La fila 1 de encabezados no existe en la hoja 'Trayectos'.");
        }

        short ultimaCelda = filaEncabezado.getLastCellNum();
        if (ultimaCelda > ENCABEZADOS_ESPERADOS.size()) {
            for (int col = ENCABEZADOS_ESPERADOS.size(); col < ultimaCelda; col++) {
                Cell extra = filaEncabezado.getCell(col);
                if (extra != null && !dataFormatter.formatCellValue(extra).isBlank()) {
                    throw new ReglaNegocioException("La fila 1 debe tener exactamente los 7 encabezados requeridos.");
                }
            }
        }

        for (int col = 0; col < ENCABEZADOS_ESPERADOS.size(); col++) {
            Cell cell = filaEncabezado.getCell(col);
            String valor = (cell == null) ? "" : dataFormatter.formatCellValue(cell);
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
}
