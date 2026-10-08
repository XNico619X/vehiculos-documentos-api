package com.utolima.vehiculosdocumentosapi.service;

import com.utolima.vehiculosdocumentosapi.dto.TrayectoRequestDTO;
import com.utolima.vehiculosdocumentosapi.model.Persona;
import com.utolima.vehiculosdocumentosapi.model.Trayecto;
import com.utolima.vehiculosdocumentosapi.model.Vehiculo;
import com.utolima.vehiculosdocumentosapi.model.VehiculoDocumento;
import com.utolima.vehiculosdocumentosapi.model.VehiculoPersona;
import com.utolima.vehiculosdocumentosapi.repository.PersonaRepository;
import com.utolima.vehiculosdocumentosapi.repository.TrayectoRepository;
import com.utolima.vehiculosdocumentosapi.repository.VehiculoDocumentoRepository;
import com.utolima.vehiculosdocumentosapi.repository.VehiculoPersonaRepository;
import com.utolima.vehiculosdocumentosapi.repository.VehiculoRepository;
import com.utolima.vehiculosdocumentosapi.service.TrayectoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoPersona;
import java.util.List;

@Service
public class TrayectoServiceImpl implements TrayectoService {

    @Autowired
    private TrayectoRepository trayectoRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private VehiculoPersonaRepository vehiculoPersonaRepository;

    @Autowired
    private VehiculoDocumentoRepository vehiculoDocumentoRepository;

    @Override
    @Transactional
    public void crearRuta(List<TrayectoRequestDTO> paradas) {
        // 1. Validar cantidad de paradas (minimo 2: inicial y final, maximo 7: hasta 5 intermedias)[cite: 5]
        if (paradas == null || paradas.size() < 2 || paradas.size() > 7) {
            throw new IllegalArgumentException("La ruta debe tener una parada inicial, una final y maximo 5 intermedias (entre 2 y 7 paradas en total).");
        }

        TrayectoRequestDTO base = paradas.get(0);

        Persona conductor = personaRepository.findById(base.getIdPersona())
                .orElseThrow(() -> new RuntimeException("Conductor no encontrado con ID: " + base.getIdPersona()));

        Vehiculo vehiculo = vehiculoRepository.findById(base.getIdVehiculo())
                .orElseThrow(() -> new RuntimeException("Vehiculo no encontrado con ID: " + base.getIdVehiculo()));

        // 2. Validar que la persona sea de tipo conductor ('C')[cite: 5, 8]
        if (conductor.getTipoPersona() != TipoPersona.CONDUCTOR) {
            throw new IllegalArgumentException("Solo deben asociarse personas de tipo CONDUCTOR a los trayectos.");
        }

        // 3. Validacion Doble A: El conductor debe estar en estado 'PO' para este vehiculo[cite: 6, 8]
        VehiculoPersona vehiculoPersona = vehiculoPersonaRepository
                .findByVehiculoAndPersona(vehiculo.getId(), conductor.getIdPersona())
                .orElseThrow(() -> new IllegalStateException("El conductor no esta asociado a este vehiculo."));

        if (vehiculoPersona.getEstadoConductor() == null
            || !"PO".equalsIgnoreCase(vehiculoPersona.getEstadoConductor().name())) {
            throw new IllegalStateException("El conductor no se encuentra en estado 'PO' (Permitido para Operar) para este vehiculo.");
        }

        // 4. Validacion Doble B: El vehiculo debe tener al menos un documento en estado 'Habilitado'[cite: 6]
        List<VehiculoDocumento> documentos = vehiculoDocumentoRepository.findByVehiculoId(vehiculo.getId());

        boolean vehiculoHabilitado = documentos.stream()
                .anyMatch(vd -> vd.getEstadoDocumento() != null
                        && "Habilitado".equalsIgnoreCase(vd.getEstadoDocumento().name()));

        if (!vehiculoHabilitado) {
            throw new IllegalStateException("El vehiculo no cuenta con documentos en estado 'Habilitado'.");
        }

        // 5. Persistir cada parada del trayecto[cite: 5, 6]
        for (TrayectoRequestDTO dto : paradas) {
            Trayecto trayecto = new Trayecto();
            trayecto.setConductor(conductor);
            trayecto.setVehiculo(vehiculo);
            trayecto.setCodigoRuta(dto.getCodigoRuta());
            trayecto.setUbicacion(dto.getUbicacion());
            trayecto.setOrdenParada(dto.getOrdenParada());
            trayecto.setLatitud(dto.getLatitud());
            trayecto.setLongitud(dto.getLongitud());
            trayecto.setLoginUsuario(dto.getLoginUsuario());

            trayectoRepository.save(trayecto);
        }
    }
}