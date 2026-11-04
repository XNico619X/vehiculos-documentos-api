package com.utolima.vehiculosdocumentosapi.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utolima.vehiculosdocumentosapi.dto.PersonaRequestDTO;
import com.utolima.vehiculosdocumentosapi.dto.PersonaResponseDTO;
import com.utolima.vehiculosdocumentosapi.exception.ReglaNegocioException;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoPersona;
import com.utolima.vehiculosdocumentosapi.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfiguracionInicialServiceImpl implements ConfiguracionInicialService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaService personaService; // reutiliza la logica que ya crea el usuario del administrativo

    @Override
    @Transactional(readOnly = true)
    public boolean requiereConfiguracionInicial() {
        // Solo los ADMINISTRATIVOS tienen usuario: si no hay ni uno, nadie puede iniciar sesion todavia.
        return usuarioRepository.count() == 0;
    }

    @Override
    @Transactional
    public PersonaResponseDTO crearPrimerAdministrador(PersonaRequestDTO dto) {

        // REGLA DE SEGURIDAD: este endpoint es PUBLICO, asi que solo puede funcionar mientras no exista ningun usuario.
        // Si no se comprobara, cualquiera podria crearse un administrador en un sistema ya en uso.
        if (usuarioRepository.count() > 0) {
            throw new ReglaNegocioException("El sistema ya fue configurado. Inicie sesion con un usuario existente.");
        }

        // El tipo NO lo decide quien llama: aqui siempre se crea un ADMINISTRATIVO.
        dto.setTipoPersona(TipoPersona.ADMINISTRATIVO);
        // La licencia solo aplica a conductores: se limpia por si la mandan (si no, el servicio rechazaria la peticion).
        dto.setLicenciaConduccionBase64(null);
        dto.setFechaVigenciaLicencia(null);

        // PersonaService.crear genera el login (nemotecnia), la contrasena y la APIKey, y los devuelve UNA sola vez.
        return personaService.crear(dto);
    }
}