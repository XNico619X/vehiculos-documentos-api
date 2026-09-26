package com.utolima.vehiculosdocumentosapi.service;

import java.util.List;

import com.utolima.vehiculosdocumentosapi.dto.PersonaRequestDTO;
import com.utolima.vehiculosdocumentosapi.dto.PersonaResponseDTO;

public interface PersonaService {

    PersonaResponseDTO crear(PersonaRequestDTO dto);

    PersonaResponseDTO obtenerPorId(Long id);

    List<PersonaResponseDTO> listarTodos();

    PersonaResponseDTO actualizar(Long id, PersonaRequestDTO dto);
}