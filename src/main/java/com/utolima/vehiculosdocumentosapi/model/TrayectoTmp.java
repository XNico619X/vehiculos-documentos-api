package com.utolima.vehiculosdocumentosapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Fila de la tabla temporal donde entra el Excel de cargue SIN modificar nada.
// Es la "sala de espera" de los trayectos: los procedimientos almacenados la validan
// y pasan lo bueno a la tabla trayecto.
@Entity
@Table(name = "trayecto_tmp")
@Getter
@Setter
@NoArgsConstructor
public class TrayectoTmp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT de la tabla
    @Column(name = "id_trayecto_tmp")
    private Long idTrayectoTmp;

    // Campos réplica de Trayecto: TODOS String, porque el Excel entra tal cual viene
    // (si fueran Long/BigDecimal, un "abc" en id_persona haría fallar la carga en vez de quedar marcado como ERROR).
    @Column(name = "id_persona")
    private String idPersona;

    @Column(name = "id_vehiculo")
    private String idVehiculo;

    @Column(name = "codigo_ruta")
    private String codigoRuta;

    @Column(name = "ubicacion")
    private String ubicacion;

    @Column(name = "orden_parada")
    private String ordenParada;

    @Column(name = "latitud")
    private String latitud;

    @Column(name = "longitud")
    private String longitud;

    @Column(name = "login_usuario")
    private String loginUsuario;

    // CARGADO, VALIDADO, PROCESADO o ERROR. Es String y NO @Enumerated: con ddl-auto=validate
    // en MariaDB, un enum espera otro tipo de columna y la app no arrancaría.
    @Column(name = "estado", nullable = false)
    private String estado;

    // Texto acumulativo de los errores que encuentra la validación para este registro.
    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    // yyyyMMddHHmm del momento del cargue (ej. 202610231530). Agrupa las filas de un mismo Excel.
    // Es Long porque 12 dígitos no caben en Integer.
    @Column(name = "id_cargue", nullable = false)
    private Long idCargue;
}