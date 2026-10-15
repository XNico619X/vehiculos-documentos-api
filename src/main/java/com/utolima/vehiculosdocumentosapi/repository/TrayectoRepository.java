package com.utolima.vehiculosdocumentosapi.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.utolima.vehiculosdocumentosapi.model.Trayecto;

import jakarta.transaction.Transactional;

@Repository
public interface TrayectoRepository extends JpaRepository<Trayecto, Long> {
    
    // Método de utilidad para las consultas posteriores por código de ruta
    List<Trayecto> findByCodigoRutaOrderByOrdenParadaAsc(String codigoRuta);
    boolean existsByCodigoRuta(String codigoRuta); // evita crear dos rutas con el mismo código

 // Trayectos a los que les falta latitud o longitud, los más antiguos primero, máximo 50 por consulta
    List<Trayecto> findTop50ByLatitudIsNullOrLongitudIsNullOrderByIdTrayectoAsc();

    // UPDATE directo: solo toca las dos columnas de coordenadas de UNA fila
    @Modifying       // le avisa a Spring que esta @Query escribe en la base, no lee
    @Transactional  // un UPDATE necesita transacción; la declaramos aquí porque el scheduler no abre ninguna
    @Query("UPDATE Trayecto t SET t.latitud = :lat, t.longitud = :lng WHERE t.idTrayecto = :id")
    int actualizarCoordenadas(@Param("id") Long id, @Param("lat") BigDecimal lat, @Param("lng") BigDecimal lng);
        // ===================== PARTE 3: CONSULTAS DE RUTAS =====================

    // CONSULTA 2: códigos de ruta de un conductor, sin repetir.
    // - DISTINCT: un conductor tiene varios trayectos por ruta (uno por parada); sin esto el código saldría repetido.
    // - t.conductor.identificacion: navega Trayecto -> Persona y filtra por el número de documento
    //   (el campo "identificacion", no el id de la tabla), como pide el enunciado.
    // - Devuelve List<String> porque solo seleccionamos una columna, no la entidad completa.
    @Query("SELECT DISTINCT t.codigoRuta FROM Trayecto t " +
           "WHERE t.conductor.identificacion = :identificacion " +
           "ORDER BY t.codigoRuta")
    List<String> findCodigosRutaPorIdentificacionConductor(@Param("identificacion") String identificacion);

    // CONSULTA 3: trayectos de un vehículo por su placa, con el conductor ya cargado.
    // - JOIN FETCH t.conductor: trae el conductor en la MISMA consulta. Como la relación es LAZY,
    //   sin esto Hibernate haría una consulta extra por cada fila (problema "N+1").
    // - Ordenamos por ruta y luego por parada; en el servicio los agruparemos por código de ruta.
    @Query("SELECT t FROM Trayecto t JOIN FETCH t.conductor " +
           "WHERE t.vehiculo.placa = :placa " +
           "ORDER BY t.codigoRuta ASC, t.ordenParada ASC")
    List<Trayecto> findTrayectosPorPlacaConConductor(@Param("placa") String placa);
}
