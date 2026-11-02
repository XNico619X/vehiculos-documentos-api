package com.utolima.vehiculosdocumentosapi;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Ejecuta este método main (clic en "Run" sobre main) para generar archivos .xlsx
 * de prueba listos para usar en Postman dentro de la carpeta /postman.
 */
public class GenerarExcelPrueba {

    private static final String[] ENCABEZADOS = {
            "id_persona",
            "id_vehiculo",
            "codigo_ruta",
            "ubicacion",
            "orden_parada",
            "latitud",
            "longitud"
    };

    public static void main(String[] args) throws IOException {
        Path carpeta = Paths.get("postman");
        Files.createDirectories(carpeta);

        // 1. Archivo con una ruta 100% válida (2 paradas para id_persona=1, id_vehiculo=1)
        Path archivoExitoso = carpeta.resolve("cargue_exitoso.xlsx");
        crearExcel(archivoExitoso, new String[][] {
                { "1", "1", "RUTA-EXCEL-01", "Terminal de Transportes de Ibague", "0", "4.438889", "-75.232222" },
                { "1", "1", "RUTA-EXCEL-01", "Plaza de Bolivar Ibague", "1", "4.444722", "-75.242500" },
                { "1", "1", "RUTA-EXCEL-01", "Universidad del Tolima", "2", "4.426500", "-75.214300" }
        });

        // 2. Archivo mixto: una ruta válida (RUTA-EXCEL-02) y una ruta con errores (RUTA-CON-ERROR)
        Path archivoMixto = carpeta.resolve("cargue_mixto.xlsx");
        crearExcel(archivoMixto, new String[][] {
                // Ruta válida (3 paradas)
                { "1", "1", "RUTA-EXCEL-02", "Parque Murillo Toro", "0", "4.443800", "-75.241100" },
                { "1", "1", "RUTA-EXCEL-02", "Estadio Manuel Murillo Toro", "1", "4.430100", "-75.207800" },
                // Ruta con errores (solo 1 parada y conductor inexistente 999)
                { "999", "1", "RUTA-CON-ERROR", "Aeropuerto Perales", "0", "4.421600", "-75.133300" }
        });

        System.out.println("=================================================================");
        System.out.println("Archivos Excel generados exitosamente en:");
        System.out.println("1) " + archivoExitoso.toAbsolutePath());
        System.out.println("2) " + archivoMixto.toAbsolutePath());
        System.out.println("=================================================================");
    }

    private static void crearExcel(Path destino, String[][] filas) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(destino.toFile())) {

            Sheet sheet = workbook.createSheet("Trayectos");

            // Fila 1 (índice 0): Encabezados exactos
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < ENCABEZADOS.length; col++) {
                headerRow.createCell(col).setCellValue(ENCABEZADOS[col]);
            }

            // Filas de datos
            for (int i = 0; i < filas.length; i++) {
                Row row = sheet.createRow(i + 1);
                String[] datosFila = filas[i];
                for (int col = 0; col < datosFila.length; col++) {
                    if (datosFila[col] != null) {
                        row.createCell(col).setCellValue(datosFila[col]);
                    }
                }
            }

            workbook.write(out);
        }
    }
}

