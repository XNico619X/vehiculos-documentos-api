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
 * Clase utilitaria para generar archivos Excel (.xlsx) de prueba en la carpeta postman/.
 *
 * Para generarlos:
 * Clic derecho sobre esta clase en el IDE -> Run Java Application (o clic en "Run" sobre el metodo main).
 *
 * NOTA IMPORTANTE SOBRE LOS DATOS DE PRUEBA:
 * Los archivos generados utilizan id_persona=1 e id_vehiculo=1, que corresponden a los datos
 * semilla de prueba de la base de datos local (donde la persona 1 es conductor habilitado 'PO'
 * y el vehiculo 1 tiene documentos habilitados). Si se prueba en otra base de datos, asegurarse
 * de que existan el conductor (tipo CONDUCTOR y estado PO) y el vehiculo asociado.
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

        // 1. Archivo con una ruta 100% valida (3 paradas para id_persona=1, id_vehiculo=1)
        Path archivoExitoso = carpeta.resolve("cargue_exitoso.xlsx");
        crearExcel(archivoExitoso, "Trayectos", ENCABEZADOS, new String[][] {
                { "1", "1", "RUTA-EXCEL-01", "Terminal de Transportes de Ibague", "0", "4.438889", "-75.232222" },
                { "1", "1", "RUTA-EXCEL-01", "Plaza de Bolivar Ibague", "1", "4.444722", "-75.242500" },
                { "1", "1", "RUTA-EXCEL-01", "Universidad del Tolima", "2", "4.426500", "-75.214300" }
        });

        // 2. Archivo mixto: una ruta valida (2 paradas) y una ruta con error (1 parada y conductor inexistente 999)
        Path archivoMixto = carpeta.resolve("cargue_mixto.xlsx");
        crearExcel(archivoMixto, "Trayectos", ENCABEZADOS, new String[][] {
                // Ruta valida (2 paradas)
                { "1", "1", "RUTA-EXCEL-02", "Parque Murillo Toro", "0", "4.443800", "-75.241100" },
                { "1", "1", "RUTA-EXCEL-02", "Estadio Manuel Murillo Toro", "1", "4.430100", "-75.207800" },
                // Ruta con errores (solo 1 parada y conductor 999)
                { "999", "1", "RUTA-CON-ERROR", "Aeropuerto Perales", "0", "4.421600", "-75.133300" }
        });

        // 3. Archivo con encabezados invalidos (para prueba de error 400)
        Path archivoEncabezadoInvalido = carpeta.resolve("cargue_encabezado_invalido.xlsx");
        crearExcel(archivoEncabezadoInvalido, "Trayectos", new String[] {
                "idPersona", "idVehiculo", "codigoRuta", "ubicacion", "orden", "lat", "lon"
        }, new String[][] {
                { "1", "1", "RUTA-ERR-01", "Punto A", "0", "4.44", "-75.24" }
        });

        // 4. Archivo con mas de 500 filas (para prueba de error 400 por exceder limite)
        Path archivo501Filas = carpeta.resolve("cargue_mas_de_500_filas.xlsx");
        String[][] filas501 = new String[501][7];
        for (int i = 0; i < 501; i++) {
            filas501[i] = new String[] {
                    "1", "1", "RUTA-LIMITE", "Parada " + i, String.valueOf(i % 5), "4.44", "-75.24"
            };
        }
        crearExcel(archivo501Filas, "Trayectos", ENCABEZADOS, filas501);

        System.out.println("=================================================================");
        System.out.println("Archivos Excel de prueba generados exitosamente en:");
        System.out.println("1) " + archivoExitoso.toAbsolutePath());
        System.out.println("2) " + archivoMixto.toAbsolutePath());
        System.out.println("3) " + archivoEncabezadoInvalido.toAbsolutePath());
        System.out.println("4) " + archivo501Filas.toAbsolutePath());
        System.out.println("=================================================================");
    }

    private static void crearExcel(Path destino, String nombreHoja, String[] encabezados, String[][] filas) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(destino.toFile())) {

            Sheet sheet = workbook.createSheet(nombreHoja);

            // Fila 1 (indice 0): Encabezados
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < encabezados.length; col++) {
                headerRow.createCell(col).setCellValue(encabezados[col]);
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
