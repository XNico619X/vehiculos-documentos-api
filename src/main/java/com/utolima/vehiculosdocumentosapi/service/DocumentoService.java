package com.utolima.vehiculosdocumentosapi.service;

import java.util.List;

import com.utolima.vehiculosdocumentosapi.model.Documento;

public interface DocumentoService {

    Documento crear(Documento documento);

    Documento obtenerPorId(Long id);

    List<Documento> listarTodos();

    Documento actualizar(Long id, Documento documento);

    void eliminar(Long id);
}