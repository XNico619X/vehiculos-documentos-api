package com.utolima.vehiculosdocumentosapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "trayecto")
@Getter
@Setter
@NoArgsConstructor
public class Trayecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_trayecto")
    private Long idTrayecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona", nullable = false)
    private Persona conductor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vehiculo", nullable = false)
    private Vehiculo vehiculo;

    @Column(name = "codigo_ruta", nullable = false, length = 50)
    private String codigoRuta;

    @Column(name = "ubicacion", nullable = false)
    private String ubicacion;

    @Column(name = "orden_parada", nullable = false)
    private Integer ordenParada;

    @Column(name = "latitud", precision = 10, scale = 8)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 11, scale = 8)
    private BigDecimal longitud;

    @Column(name = "login_usuario", nullable = false, length = 50)
    private String loginUsuario;
}