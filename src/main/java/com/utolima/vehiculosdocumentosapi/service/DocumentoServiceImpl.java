package com.utolima.vehiculosdocumentosapi.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utolima.vehiculosdocumentosapi.exception.RecursoNoEncontradoException;
import com.utolima.vehiculosdocumentosapi.model.Documento;
import com.utolima.vehiculosdocumentosapi.repository.DocumentoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // Lombok genera automaticamente un constructor con todos los campos "final" -- reemplaza el constructor que escribimos a mano en VehiculoServiceImpl
public class DocumentoServiceImpl implements DocumentoService {

    private final DocumentoRepository documentoRepository; // Lombok ve este campo final y lo mete en el constructor generado

    @Override
    @Transactional
    public Documento crear(Documento documento) {
        return documentoRepository.save(documento);
    }

    @Override
    public Documento obtenerPorId(Long id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el documento con id " + id));
    }

    @Override
    public List<Documento> listarTodos() {
        return documentoRepository.findAll();
    }

    @Override
    @Transactional
    public Documento actualizar(Long id, Documento documento) {
        Documento existente = obtenerPorId(id); // valida que exista antes de actualizar
        existente.setCodigoDocumento(documento.getCodigoDocumento());
        existente.setNombreDocumento(documento.getNombreDocumento());
        existente.setTipoVehiculoAplica(documento.getTipoVehiculoAplica());
        existente.setObligatoriedad(documento.getObligatoriedad());
        existente.setDescripcion(documento.getDescripcion());
        return documentoRepository.save(existente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Documento documento = obtenerPorId(id);
        documentoRepository.delete(documento);
    }
}