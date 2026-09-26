package com.utolima.vehiculosdocumentosapi.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utolima.vehiculosdocumentosapi.dto.PersonaRequestDTO;
import com.utolima.vehiculosdocumentosapi.dto.PersonaResponseDTO;
import com.utolima.vehiculosdocumentosapi.exception.RecursoNoEncontradoException;
import com.utolima.vehiculosdocumentosapi.mapper.PersonaMapper;
import com.utolima.vehiculosdocumentosapi.model.Persona;
import com.utolima.vehiculosdocumentosapi.model.TipoPersona;
import com.utolima.vehiculosdocumentosapi.model.Usuario;
import com.utolima.vehiculosdocumentosapi.model.UsuarioId;
import com.utolima.vehiculosdocumentosapi.repository.PersonaRepository;
import com.utolima.vehiculosdocumentosapi.repository.UsuarioRepository;

@Service
public class PersonaServiceImpl implements PersonaService {

    private final PersonaRepository personaRepository;
    private final UsuarioRepository usuarioRepository;

    public PersonaServiceImpl(PersonaRepository personaRepository,
                               UsuarioRepository usuarioRepository) {
        this.personaRepository = personaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public PersonaResponseDTO crear(PersonaRequestDTO dto) {
        Persona persona = new Persona();
        persona.setIdentificacion(dto.getIdentificacion());
        persona.setTipoIdentificacion(dto.getTipoIdentificacion());
        persona.setNombres(dto.getNombres());
        persona.setApellidos(dto.getApellidos());
        persona.setCorreoElectronico(dto.getCorreoElectronico());
        persona.setTipoPersona(dto.getTipoPersona());

        Persona personaGuardada = personaRepository.save(persona);

        // regla clave del PDF: solo ADMINISTRATIVO recibe Usuario, y es obligatorio para ese caso
        Usuario usuarioGuardado = null;
        if (personaGuardada.getTipoPersona() == TipoPersona.ADMINISTRATIVO) {
            usuarioGuardado = crearUsuarioParaPersona(personaGuardada);
        }

        // incluirPassword = true: es la única vez que la mostramos
        return PersonaMapper.toResponseDTO(personaGuardada, usuarioGuardado, true);
    }

    @Override
    public PersonaResponseDTO obtenerPorId(Long id) {
        Persona persona = buscarPersonaOLanzar(id);
        Usuario usuario = usuarioRepository.findByIdIdPersona(id).orElse(null);
        return PersonaMapper.toResponseDTO(persona, usuario, false);
    }

    @Override
    public List<PersonaResponseDTO> listarTodos() {
        return personaRepository.findAll().stream()
                .map(p -> PersonaMapper.toResponseDTO(
                        p,
                        usuarioRepository.findByIdIdPersona(p.getIdPersona()).orElse(null),
                        false))
                .toList();
    }

    @Override
    @Transactional
    public PersonaResponseDTO actualizar(Long id, PersonaRequestDTO dto) {
        Persona persona = buscarPersonaOLanzar(id);
        persona.setIdentificacion(dto.getIdentificacion());
        persona.setTipoIdentificacion(dto.getTipoIdentificacion());
        persona.setNombres(dto.getNombres());
        persona.setApellidos(dto.getApellidos());
        persona.setCorreoElectronico(dto.getCorreoElectronico());
        persona.setTipoPersona(dto.getTipoPersona());

        Persona personaActualizada = personaRepository.save(persona);
        Usuario usuario = usuarioRepository.findByIdIdPersona(id).orElse(null);

        return PersonaMapper.toResponseDTO(personaActualizada, usuario, false);
    }

    private Persona buscarPersonaOLanzar(Long id) {
        return personaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la persona con id " + id));
    }

    private Usuario crearUsuarioParaPersona(Persona persona) {
        String login = generarLogin(persona);

        Usuario usuario = new Usuario();
        usuario.setId(new UsuarioId(persona.getIdPersona(), login));
        usuario.setPersona(persona);
        usuario.setPassword(generarPassword());
        usuario.setApikey(generarApikey());

        return usuarioRepository.save(usuario);
    }

    // nemotecnia: primera letra del nombre + primera letra del apellido + identificacion
    private String generarLogin(Persona persona) {
        String inicialNombre = persona.getNombres().substring(0, 1);
        String inicialApellido = persona.getApellidos().substring(0, 1);
        return (inicialNombre + inicialApellido + persona.getIdentificacion()).toLowerCase();
    }

    private String generarPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String generarApikey() {
        return UUID.randomUUID().toString();
    }
}