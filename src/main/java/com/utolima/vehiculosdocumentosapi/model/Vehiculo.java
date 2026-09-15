package com.utolima.vehiculosdocumentosapi.model;

import com.utolima.vehiculosdocumentosapi.model.enums.TipoCombustible;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoServicio;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoVehiculo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import jakarta.persistence.OneToMany;

@Entity // le dice a JPA/Hibernate: "esta clase se mapea a una tabla de la base de datos"
@Table(name = "vehiculos") // nombre exacto de la tabla ya creada con el script SQL
@Getter // Lombok genera automaticamente todos los getters (getId(), getPlaca(), etc.)
@Setter // Lombok genera automaticamente todos los setters (setId(), setPlaca(), etc.)
@NoArgsConstructor // Lombok genera un constructor vacio Vehiculo() -> JPA lo exige internamente
@AllArgsConstructor // Lombok genera un constructor con todos los campos, util para pruebas/DTOs
public class Vehiculo {

    @Id // marca este campo como la llave primaria de la tabla
    @GeneratedValue(strategy = GenerationType.IDENTITY) // IDENTITY = usa el AUTO_INCREMENT de MariaDB
    @Column(name = "id_vehiculo") // nombre exacto de la columna PK en la tabla
    private Long id;

    @NotNull // validacion de Bean Validation: no puede llegar null en el request
    @Column(name = "tipo_vehiculo", nullable = false, length = 15) // debe coincidir con VARCHAR(15) NOT NULL
    private TipoVehiculo tipoVehiculo; // el converter que hicimos traduce esto a "AUTOMOVIL"/"MOTOCICLETA"

    @NotNull
    @Size(min = 6, max = 6) // valida en Java que tenga exactamente 6 caracteres (redundante con el CHECK, pero da error mas claro antes de llegar a la BD)
    @Column(name = "placa", nullable = false, unique = true, length = 6) // unique=true refuerza a nivel de JPA la restriccion UNIQUE
    private String placa;

    @NotNull
    @Column(name = "tipo_servicio", nullable = false, length = 2)
    private TipoServicio tipoServicio;

    @NotNull
    @Column(name = "tipo_combustible", nullable = false, length = 15)
    private TipoCombustible tipoCombustible;

    @NotNull
    @Column(name = "capacidad_pasajeros", nullable = false)
    private Integer capacidadPasajeros;

    @NotNull
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") // valida formato hexadecimal tipo #FFFFFF antes de llegar a la BD
    @Column(name = "color", nullable = false, length = 7)
    private String color;

    @NotNull
    @Column(name = "modelo", nullable = false)
    private Integer modelo;

    @NotNull
    @Column(name = "marca", nullable = false, length = 50)
    private String marca;

    @NotNull
    @Column(name = "linea", nullable = false, length = 50)
    private String linea;
    
    @OneToMany(mappedBy = "vehiculo", fetch = FetchType.LAZY) // "mappedBy" apunta al nombre del campo en VehiculoDocumento
    private List<VehiculoDocumento> documentosAsociados;
}
