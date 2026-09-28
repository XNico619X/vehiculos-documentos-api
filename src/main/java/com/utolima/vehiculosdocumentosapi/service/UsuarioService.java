package com.utolima.vehiculosdocumentosapi.service;

import com.utolima.vehiculosdocumentosapi.dto.CambioPasswordDTO;
import com.utolima.vehiculosdocumentosapi.model.Usuario;

public interface UsuarioService {
    Usuario cambiarPassword(String login, CambioPasswordDTO dto);
    Usuario regenerarApiKey(String login);
}