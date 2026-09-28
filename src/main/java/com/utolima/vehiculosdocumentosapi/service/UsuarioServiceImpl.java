package com.utolima.vehiculosdocumentosapi.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.utolima.vehiculosdocumentosapi.dto.CambioPasswordDTO;
import com.utolima.vehiculosdocumentosapi.exception.RecursoNoEncontradoException;
import com.utolima.vehiculosdocumentosapi.model.Usuario;
import com.utolima.vehiculosdocumentosapi.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public Usuario cambiarPassword(String login, CambioPasswordDTO dto) {
        Usuario usuario = usuarioRepository.findByIdLogin(login)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con login " + login));

        // Verificación del password actual
        if (!usuario.getPassword().equals(dto.getPasswordActual())) {
            throw new IllegalArgumentException("El password actual ingresado es incorrecto.");
        }

        usuario.setPassword(dto.getPasswordNuevo());
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public Usuario regenerarApiKey(String login) {
        Usuario usuario = usuarioRepository.findByIdLogin(login)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con login " + login));

        // Generación de un nuevo APIKey seguro utilizando UUID
        String nuevoApiKey = UUID.randomUUID().toString().replace("-", "");
        usuario.setApikey(nuevoApiKey);
        
        return usuarioRepository.save(usuario);
    }
}