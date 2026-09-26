package com.utolima.vehiculosdocumentosapi.mapper;

import com.utolima.vehiculosdocumentosapi.dto.PersonaResponseDTO;
import com.utolima.vehiculosdocumentosapi.dto.UsuarioResponseDTO;
import com.utolima.vehiculosdocumentosapi.model.Persona;
import com.utolima.vehiculosdocumentosapi.model.Usuario;

public class PersonaMapper {

    // incluirPassword: true solo justo al crear el usuario (para mostrarla una vez)
    public static PersonaResponseDTO toResponseDTO(Persona persona, Usuario usuario, boolean incluirPassword) {
        PersonaResponseDTO dto = new PersonaResponseDTO();
        dto.setIdPersona(persona.getIdPersona());
        dto.setIdentificacion(persona.getIdentificacion());
        dto.setTipoIdentificacion(persona.getTipoIdentificacion());
        dto.setNombres(persona.getNombres());
        dto.setApellidos(persona.getApellidos());
        dto.setCorreoElectronico(persona.getCorreoElectronico());
        dto.setTipoPersona(persona.getTipoPersona());

        if (usuario != null) {
            UsuarioResponseDTO usuarioDTO = new UsuarioResponseDTO();
            usuarioDTO.setLogin(usuario.getId().getLogin());
            usuarioDTO.setApikey(usuario.getApikey());
            usuarioDTO.setPassword(incluirPassword ? usuario.getPassword() : null);
            dto.setUsuario(usuarioDTO);
        }

        return dto;
    }
}