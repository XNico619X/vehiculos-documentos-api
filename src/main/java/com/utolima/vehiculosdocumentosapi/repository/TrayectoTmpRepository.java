package com.utolima.vehiculosdocumentosapi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utolima.vehiculosdocumentosapi.model.TrayectoTmp;

@Repository
public interface TrayectoTmpRepository extends JpaRepository<TrayectoTmp, Long> {

    // Todas las filas de un cargue, en el orden en que entraron (por id).
    // Spring Data arma el SQL a partir del nombre del método.
    List<TrayectoTmp> findByIdCargueOrderByIdTrayectoTmpAsc(Long idCargue);
}