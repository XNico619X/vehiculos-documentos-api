package com.utolima.vehiculosdocumentosapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.utolima.vehiculosdocumentosapi.model.Documento;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    // No necesita metodos extra por ahora: el CRUD basico de JpaRepository cubre
    // el "Realizar el CRUD (POST-GET-DELETE-PUT) de la entidad parametrica de documentos"
}