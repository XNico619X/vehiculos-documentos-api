package com.utolima.vehiculosdocumentosapi.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utolima.vehiculosdocumentosapi.dto.TrayectoRequestDTO;
import com.utolima.vehiculosdocumentosapi.exception.PersonaNoEsConductorException;
import com.utolima.vehiculosdocumentosapi.exception.RecursoNoEncontradoException;
import com.utolima.vehiculosdocumentosapi.exception.ReglaNegocioException;
import com.utolima.vehiculosdocumentosapi.model.Persona;
import com.utolima.vehiculosdocumentosapi.model.Trayecto;
import com.utolima.vehiculosdocumentosapi.model.Usuario;
import com.utolima.vehiculosdocumentosapi.model.Vehiculo;
import com.utolima.vehiculosdocumentosapi.model.VehiculoDocumento;
import com.utolima.vehiculosdocumentosapi.model.VehiculoPersona;
import com.utolima.vehiculosdocumentosapi.model.VehiculoPersonaId;
import com.utolima.vehiculosdocumentosapi.model.enums.EstadoConductor;
import com.utolima.vehiculosdocumentosapi.model.enums.EstadoDocumento;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoPersona;
import com.utolima.vehiculosdocumentosapi.repository.PersonaRepository;
import com.utolima.vehiculosdocumentosapi.repository.TrayectoRepository;
import com.utolima.vehiculosdocumentosapi.repository.VehiculoDocumentoRepository;
import com.utolima.vehiculosdocumentosapi.repository.VehiculoPersonaRepository;
import com.utolima.vehiculosdocumentosapi.repository.VehiculoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // reemplaza los @Autowired de campo: mismo estilo que el resto del proyecto
public class TrayectoServiceImpl implements TrayectoService {

    private static final int MIN_PARADAS = 2; // parada inicial + parada final
    private static final int MAX_PARADAS = 7; // inicial + hasta 5 intermedias + final

    private final TrayectoRepository trayectoRepository;
    private final PersonaRepository personaRepository;
    private final VehiculoRepository vehiculoRepository;
    private final VehiculoPersonaRepository vehiculoPersonaRepository;
    private final VehiculoDocumentoRepository vehiculoDocumentoRepository;

    @Override
    @Transactional // si falla cualquier parada no se guarda ninguna: la ruta se registra completa o no se registra
    public void crearRuta(List<TrayectoRequestDTO> paradas) {

        // 1. Forma de la ruta: cantidad, campos obligatorios, mismo conductor/vehículo/código, órdenes 0..n-1
        validarEstructuraDeRuta(paradas);

        TrayectoRequestDTO base = paradas.get(0); // ya validamos que todas las paradas comparten estos datos

        // 2. Que existan el conductor y el vehículo (404 si no)
        Persona conductor = personaRepository.findById(base.getIdPersona())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el conductor con id " + base.getIdPersona()));
        Vehiculo vehiculo = vehiculoRepository.findById(base.getIdVehiculo())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el vehículo con id " + base.getIdVehiculo()));

        // 3. Solo personas tipo CONDUCTOR (reutiliza la excepción que ya existe)
        if (conductor.getTipoPersona() != TipoPersona.CONDUCTOR) {
            throw new PersonaNoEsConductorException("Solo se pueden registrar trayectos a personas de tipo CONDUCTOR. "
                    + "La persona " + conductor.getIdPersona() + " es de tipo " + conductor.getTipoPersona());
        }

        // 4. Regla A de la nota del PDF: el conductor debe poder operar (PO) ESTE vehículo
        VehiculoPersona asociacion = vehiculoPersonaRepository
                .findById(new VehiculoPersonaId(vehiculo.getId(), conductor.getIdPersona())) // llave compuesta, sin @Query
                .orElseThrow(() -> new ReglaNegocioException("El conductor no está asociado a este vehículo."));

        // se compara contra la CONSTANTE del enum: name() devuelve "PUEDE_OPERAR", nunca "PO"
        if (asociacion.getEstadoConductor() != EstadoConductor.PUEDE_OPERAR) {
            throw new ReglaNegocioException("El conductor no está en estado PO (Puede Operar) para este vehículo.");
        }

        // 5. Regla B de la nota: los documentos del vehículo deben estar Habilitados.
        // allMatch (todos, no solo uno) + exigir que tenga al menos uno
        List<VehiculoDocumento> documentos = vehiculoDocumentoRepository.findByVehiculo_Id(vehiculo.getId());
        boolean vehiculoHabilitado = !documentos.isEmpty()
                && documentos.stream().allMatch(vd -> vd.getEstadoDocumento() == EstadoDocumento.HABILITADO);
        if (!vehiculoHabilitado) {
            throw new ReglaNegocioException("El vehículo no tiene todos sus documentos en estado Habilitado.");
        }

        // 6. El login sale del usuario autenticado (token), no del body: el cliente no puede falsificarlo
        String loginQuienRegistra = obtenerLoginAutenticado();

        // 7. Guardar todas las paradas
        for (TrayectoRequestDTO dto : paradas) {
            Trayecto trayecto = new Trayecto();
            trayecto.setConductor(conductor);
            trayecto.setVehiculo(vehiculo);
            trayecto.setCodigoRuta(dto.getCodigoRuta());
            trayecto.setUbicacion(dto.getUbicacion());
            trayecto.setOrdenParada(dto.getOrdenParada());
            trayecto.setLatitud(dto.getLatitud());   // puede ser null: la tarea de Google Maps la completa luego
            trayecto.setLongitud(dto.getLongitud());
            trayecto.setLoginUsuario(loginQuienRegistra);
            trayectoRepository.save(trayecto);
        }
    }

    // Validaciones que no dependen de la base de datos
    private void validarEstructuraDeRuta(List<TrayectoRequestDTO> paradas) {
        if (paradas == null || paradas.size() < MIN_PARADAS || paradas.size() > MAX_PARADAS) {
            throw new ReglaNegocioException("La ruta debe tener entre " + MIN_PARADAS + " y " + MAX_PARADAS
                    + " paradas (inicial, final y máximo 5 intermedias).");
        }

        TrayectoRequestDTO base = paradas.get(0);
        Set<Integer> ordenesVistos = new HashSet<>();

        for (TrayectoRequestDTO p : paradas) {
            if (p == null || p.getIdPersona() == null || p.getIdVehiculo() == null || p.getOrdenParada() == null
                    || esVacio(p.getCodigoRuta()) || esVacio(p.getUbicacion())) {
                throw new ReglaNegocioException(
                        "Cada parada debe traer idPersona, idVehiculo, codigoRuta, ubicacion y ordenParada.");
            }
            // todas las paradas pertenecen a la MISMA ruta, conductor y vehículo
            if (!p.getIdPersona().equals(base.getIdPersona())
                    || !p.getIdVehiculo().equals(base.getIdVehiculo())
                    || !p.getCodigoRuta().equals(base.getCodigoRuta())) {
                throw new ReglaNegocioException(
                        "Todas las paradas de una ruta deben tener el mismo conductor, vehículo y código de ruta.");
            }
            // 0 = inicial, el mayor = final. Con valores únicos dentro de 0..n-1 quedan exactamente 0,1,2,...,n-1
            if (p.getOrdenParada() < 0 || p.getOrdenParada() >= paradas.size()) {
                throw new ReglaNegocioException("ordenParada debe estar entre 0 y " + (paradas.size() - 1) + ".");
            }
            if (!ordenesVistos.add(p.getOrdenParada())) { // add() devuelve false si ya estaba
                throw new ReglaNegocioException("El ordenParada " + p.getOrdenParada() + " está repetido en la ruta.");
            }
        }

        // el código de ruta agrupa paradas: no puede reutilizarse
        if (trayectoRepository.existsByCodigoRuta(base.getCodigoRuta())) {
            throw new ReglaNegocioException("Ya existe una ruta con el código " + base.getCodigoRuta() + ".");
        }
    }

    // El filtro JWT puso al Usuario completo como "principal" del SecurityContext
    private String obtenerLoginAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario usuario) {
            return usuario.getId().getLogin();
        }
        throw new ReglaNegocioException("No se pudo identificar al usuario autenticado que registra el trayecto.");
    }

    private boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}