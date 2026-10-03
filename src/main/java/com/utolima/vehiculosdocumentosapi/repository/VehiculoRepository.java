package com.utolima.vehiculosdocumentosapi.repository;
import java.util.List;

import java.time.LocalDate;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.utolima.vehiculosdocumentosapi.model.Vehiculo;
import com.utolima.vehiculosdocumentosapi.model.enums.EstadoDocumento;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoVehiculo;

// JpaRepository<Vehiculo, Long>: el primer parametro es la entidad que maneja,
// el segundo es el tipo de dato de su llave primaria (Long, porque Vehiculo.id es Long)
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    // "Query Method" derivado: Spring Data lee el nombre del metodo y genera el SQL solo.
    // findBy + Placa (el nombre exacto del campo en la entidad Vehiculo) = "busca donde placa = ?"
    Optional<Vehiculo> findByPlaca(String placa); // Optional porque puede no existir esa placa

    // findBy + TipoVehiculo = "busca donde tipoVehiculo = ?"; devuelve List porque puede haber varios
    List<Vehiculo> findByTipoVehiculo(TipoVehiculo tipoVehiculo);

    // Busqueda mas compleja: "vehiculos que tengan en comun un tipo de documento" (dado el id de un documento)
    // Aqui ya no alcanza el nombre del metodo, necesitamos JPQL explicito con @Query
    @Query("SELECT DISTINCT v FROM Vehiculo v JOIN v.documentosAsociados vd WHERE vd.documento.id = :idDocumento")
    List<Vehiculo> findVehiculosPorDocumento(@Param("idDocumento") Long idDocumento);

    // Busqueda por estado del documento asociado (Habilitado / Vencido / En Verificacion)
    @Query("SELECT DISTINCT v FROM Vehiculo v JOIN v.documentosAsociados vd WHERE vd.estadoDocumento = :estado")
    List<Vehiculo> findVehiculosPorEstadoDocumento(@Param("estado") EstadoDocumento estado);


 // "vehiculos que tengan documentos vencidos" -- reutilizamos EstadoDocumento.VENCIDO que YA existe desde la Entrega 1,
 // no hace falta un metodo nuevo: vehiculoRepository.findVehiculosPorEstadoDocumento(EstadoDocumento.VENCIDO)

 // "vehiculos que tienen documentos por vencer con un tiempo que se especifique" agregado segunda entrega
 @Query("SELECT DISTINCT vd.vehiculo FROM VehiculoDocumento vd " +
        "WHERE vd.fechaVencimiento BETWEEN CURRENT_DATE AND :fechaLimite")
 List<Vehiculo> findVehiculosConDocumentosPorVencer(@Param("fechaLimite") LocalDate fechaLimite);
 // CURRENT_DATE es una funcion de JPQL (no hay que pasarla como parametro), y fechaLimite es "hoy + N dias",
 // calculado en el servicio antes de llamar a este metodo, asi el repositorio no sabe nada de "dias", solo de fechas

}