package com.utolima.vehiculosdocumentosapi.repository;

import com.utolima.vehiculosdocumentosapi.model.Trayecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrayectoRepository extends JpaRepository<Trayecto, Long> {
    
    // Método de utilidad para las consultas posteriores por código de ruta
    List<Trayecto> findByCodigoRutaOrderByOrdenParadaAsc(String codigoRuta);
    boolean existsByCodigoRuta(String codigoRuta); // evita crear dos rutas con el mismo código
}
