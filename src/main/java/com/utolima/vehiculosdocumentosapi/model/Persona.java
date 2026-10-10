package com.utolima.vehiculosdocumentosapi.model;

import java.time.LocalDate;

import com.utolima.vehiculosdocumentosapi.model.enums.TipoIdentificacion;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoPersona;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "persona")
@Getter
@Setter
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_persona")
    private Long idPersona;

    @Column(name = "identificacion", nullable = false, unique = true)
    private String identificacion;

    @Column(name = "tipo_identificacion", nullable = false, length = 5)
    private TipoIdentificacion tipoIdentificacion;

    @Column(name = "nombres", nullable = false)
    private String nombres;

    @Column(name = "apellidos", nullable = false)
    private String apellidos;

    @Column(name = "correo_electronico", nullable = false)
    private String correoElectronico;

    @Column(name = "tipo_persona", nullable = false)
    private TipoPersona tipoPersona;

    @Lob
    @Column(name = "licencia_conduccion", columnDefinition = "LONGBLOB")
    private byte[] licenciaConduccion; // byte[] y no String: así Hibernate espera un BLOB, igual que en VehiculoDocumento

    @Column(name = "fecha_vigencia_licencia")
    private LocalDate fechaVigenciaLicencia;
}