package com.utolima.vehiculosdocumentosapi.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utolima.vehiculosdocumentosapi.dto.CambioEstadoConductorDTO;
import com.utolima.vehiculosdocumentosapi.dto.VehiculoPersonaRequestDTO;
import com.utolima.vehiculosdocumentosapi.exception.PersonaNoEsConductorException;
import com.utolima.vehiculosdocumentosapi.exception.RecursoNoEncontradoException;
import com.utolima.vehiculosdocumentosapi.model.Persona;
import com.utolima.vehiculosdocumentosapi.model.Vehiculo;
import com.utolima.vehiculosdocumentosapi.model.VehiculoPersona;
import com.utolima.vehiculosdocumentosapi.model.VehiculoPersonaId;
import com.utolima.vehiculosdocumentosapi.model.enums.EstadoConductor;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoPersona;
import com.utolima.vehiculosdocumentosapi.repository.PersonaRepository;
import com.utolima.vehiculosdocumentosapi.repository.VehiculoPersonaRepository;
import com.utolima.vehiculosdocumentosapi.repository.VehiculoRepository;

import lombok.RequiredArgsConstructor; // esta vez si usamos el atajo de Lombok, como te mostre con DocumentoServiceImpl

@Service
@RequiredArgsConstructor
public class VehiculoPersonaServiceImpl implements VehiculoPersonaService {

    private final VehiculoRepository vehiculoRepository;
    private final PersonaRepository personaRepository;
    private final VehiculoPersonaRepository vehiculoPersonaRepository;

    @Override
    @Transactional
    public VehiculoPersona asociarConductor(Long idVehiculo, VehiculoPersonaRequestDTO dto) {
        Vehiculo vehiculo = vehiculoRepository.findById(idVehiculo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el vehículo con id " + idVehiculo));

        Persona persona = personaRepository.findById(dto.getIdPersona())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la persona con id " + dto.getIdPersona()));

        // REGLA CLAVE del PDF: "Aqui se asocian unicamente personas de tipo CONDUCTOR"
        // esto NO se puede validar con un CHECK de SQL porque necesita consultar OTRA tabla (persona) --
        // por eso vive aqui, en la capa de servicio, igual que la regla de "vehiculo sin documento" en la Entrega 1
        if (persona.getTipoPersona() != TipoPersona.CONDUCTOR) {
            throw new PersonaNoEsConductorException(
                    "Solo se pueden asociar personas de tipo CONDUCTOR a un vehículo. La persona " +
                    dto.getIdPersona() + " es de tipo " + persona.getTipoPersona());
        }

        VehiculoPersona vp = new VehiculoPersona();
        vp.setVehiculo(vehiculo);   // @MapsId rellena automaticamente idVehiculo en la llave compuesta
        vp.setPersona(persona);    // @MapsId rellena automaticamente idPersona en la llave compuesta
        vp.setFechaAsociacion(dto.getFechaAsociacion());
        vp.setEstadoConductor(EstadoConductor.ESPERA_APROBACION); // regla del PDF: toda asociacion nueva inicia en EA

        return vehiculoPersonaRepository.save(vp);
    }

    @Override
    @Transactional
    public VehiculoPersona cambiarEstado(Long idVehiculo, Long idPersona, CambioEstadoConductorDTO dto) {
        VehiculoPersonaId id = new VehiculoPersonaId(idVehiculo, idPersona);

        VehiculoPersona vp = vehiculoPersonaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una asociación entre el vehículo " + idVehiculo + " y la persona " + idPersona));

        vp.setEstadoConductor(dto.getEstadoConductor());
        return vehiculoPersonaRepository.save(vp);
    }

    @Override
    public List<VehiculoPersona> listarConductoresDeVehiculo(Long idVehiculo) {
        return vehiculoPersonaRepository.findByVehiculo_Id(idVehiculo);
    }

    @Override
    public List<VehiculoPersona> listarVehiculosDeConductor(Long idPersona) {
        return vehiculoPersonaRepository.findByPersona_IdPersona(idPersona); 
    }
}