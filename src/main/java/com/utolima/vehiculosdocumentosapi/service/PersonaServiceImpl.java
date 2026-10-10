package com.utolima.vehiculosdocumentosapi.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utolima.vehiculosdocumentosapi.dto.PersonaRequestDTO;
import com.utolima.vehiculosdocumentosapi.dto.PersonaResponseDTO;
import com.utolima.vehiculosdocumentosapi.exception.RecursoNoEncontradoException;
import com.utolima.vehiculosdocumentosapi.exception.ReglaNegocioException;
import com.utolima.vehiculosdocumentosapi.mapper.PersonaMapper;
import com.utolima.vehiculosdocumentosapi.model.Persona;
import com.utolima.vehiculosdocumentosapi.model.Usuario;
import com.utolima.vehiculosdocumentosapi.model.UsuarioId;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoPersona;
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
        aplicarDatosLicencia(persona, dto);

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
        TipoPersona tipoAnterior = persona.getTipoPersona(); // guardamos el tipo ANTES de pisarlo con los datos nuevos

        persona.setIdentificacion(dto.getIdentificacion());
        persona.setTipoIdentificacion(dto.getTipoIdentificacion());
        persona.setNombres(dto.getNombres());
        persona.setApellidos(dto.getApellidos());
        persona.setCorreoElectronico(dto.getCorreoElectronico());
        persona.setTipoPersona(dto.getTipoPersona());
        aplicarDatosLicencia(persona, dto);

        Persona personaActualizada = personaRepository.save(persona);

        Usuario usuario = usuarioRepository.findByIdIdPersona(id).orElse(null);
        boolean usuarioRecienCreado = false;

        // REGLA: si pasa de CONDUCTOR (o cualquier otro tipo) a ADMINISTRATIVO y todavia no tiene Usuario, se lo creamos --
        // mismo comportamiento que en crear(), reutilizando el mismo metodo privado para no duplicar la logica de nemotecnia
        if (personaActualizada.getTipoPersona() == TipoPersona.ADMINISTRATIVO && usuario == null) {
            usuario = crearUsuarioParaPersona(personaActualizada);
            usuarioRecienCreado = true;
        }

        // incluirPassword = true SOLO si el usuario se creo en este mismo llamado -- es la unica vez que se puede mostrar
        return PersonaMapper.toResponseDTO(personaActualizada, usuario, usuarioRecienCreado);
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
 // Aplica los datos de licencia del request sobre la entidad, con sus validaciones
    private void aplicarDatosLicencia(Persona persona, PersonaRequestDTO dto) {
        boolean traeLicencia = dto.getLicenciaConduccionBase64() != null && !dto.getLicenciaConduccionBase64().isBlank();
        boolean traeFecha = dto.getFechaVigenciaLicencia() != null;

        // el PDF dice que estos campos "aplican cuando la persona es tipo conductor"
        if ((traeLicencia || traeFecha) && persona.getTipoPersona() != TipoPersona.CONDUCTOR) {
            throw new ReglaNegocioException("La licencia de conducción solo aplica a personas de tipo CONDUCTOR.");
        }

        if (traeLicencia) {
            String base64 = dto.getLicenciaConduccionBase64().trim();
            if (!base64.startsWith("JVBERi0")) { // "%PDF-" codificado en Base64
                throw new ReglaNegocioException("La licencia debe ser un PDF en Base64 válido.");
            }
            try {
                java.util.Base64.getDecoder().decode(base64); // solo comprueba que sea Base64 válido
            } catch (IllegalArgumentException e) {
                throw new ReglaNegocioException("El texto de la licencia no es Base64 válido.");
            }
            // se guarda el texto Base64 tal cual dentro de la columna LONGBLOB, igual que documento_base64
            persona.setLicenciaConduccion(base64.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        // si en un PUT no vienen estos campos, se conservan los valores que ya tenía (no se borran)
        if (traeFecha) {
            persona.setFechaVigenciaLicencia(dto.getFechaVigenciaLicencia());
        }
    }
}