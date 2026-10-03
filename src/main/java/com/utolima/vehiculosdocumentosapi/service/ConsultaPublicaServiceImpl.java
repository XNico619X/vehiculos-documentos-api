package com.utolima.vehiculosdocumentosapi.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.utolima.vehiculosdocumentosapi.dto.ConductorAsociadoResponseDTO;
import com.utolima.vehiculosdocumentosapi.dto.ConteoPersonaPorTipoDTO;
import com.utolima.vehiculosdocumentosapi.dto.DocumentoAsociadoResponseDTO;
import com.utolima.vehiculosdocumentosapi.dto.VehiculoDetalleCompletoDTO;
import com.utolima.vehiculosdocumentosapi.exception.RecursoNoEncontradoException;
import com.utolima.vehiculosdocumentosapi.model.Persona;
import com.utolima.vehiculosdocumentosapi.model.Vehiculo;
import com.utolima.vehiculosdocumentosapi.model.VehiculoDocumento;
import com.utolima.vehiculosdocumentosapi.model.VehiculoPersona;
import com.utolima.vehiculosdocumentosapi.model.enums.EstadoConductor;
import com.utolima.vehiculosdocumentosapi.model.enums.EstadoDocumento;
import com.utolima.vehiculosdocumentosapi.repository.PersonaRepository;
import com.utolima.vehiculosdocumentosapi.repository.VehiculoPersonaRepository;
import com.utolima.vehiculosdocumentosapi.repository.VehiculoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConsultaPublicaServiceImpl implements ConsultaPublicaService {

    private final VehiculoRepository vehiculoRepository;
    private final PersonaRepository personaRepository;
    private final VehiculoPersonaRepository vehiculoPersonaRepository;

    @Override
    public List<Vehiculo> vehiculosConDocumentosVencidos() {
        // reutiliza el metodo de busqueda que ya existia desde la Entrega 1
        return vehiculoRepository.findVehiculosPorEstadoDocumento(EstadoDocumento.VENCIDO);
    }

    @Override
    public List<Vehiculo> vehiculosConDocumentosPorVencer(int dias) {
        LocalDate fechaLimite = LocalDate.now().plusDays(dias); // "hoy + N dias" se calcula aqui, no en el repositorio
        return vehiculoRepository.findVehiculosConDocumentosPorVencer(fechaLimite);
    }

    @Override
    public List<Persona> conductoresQuePuedenOperar() {
        return vehiculoPersonaRepository.findPersonasPorEstadoConductor(EstadoConductor.PUEDE_OPERAR);
    }

    @Override
    public VehiculoDetalleCompletoDTO vehiculoCompletoPorPlaca(String placa) {
        Vehiculo vehiculo = vehiculoRepository.findByPlaca(placa)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un vehículo con placa " + placa));

        // armamos la lista de documentos -- mismo patron que VehiculoMapper.toResponseDTO
        List<DocumentoAsociadoResponseDTO> documentos = vehiculo.getDocumentosAsociados().stream()
                .map(vd -> new DocumentoAsociadoResponseDTO(
                        vd.getDocumento().getId(),
                        vd.getDocumento().getCodigoDocumento(),
                        vd.getDocumento().getNombreDocumento(),
                        vd.getFechaExpedicion(),
                        vd.getFechaVencimiento(),
                        vd.getEstadoDocumento()))
                .toList();

        // armamos la lista de conductores asociados, consultando VehiculoPersonaRepository por el id del vehiculo
        List<ConductorAsociadoResponseDTO> conductores = vehiculoPersonaRepository.findByVehiculo_Id(vehiculo.getId())
                .stream()
                .map(vp -> new ConductorAsociadoResponseDTO(
                        vp.getPersona().getIdPersona(),
                        vp.getPersona().getIdentificacion(),
                        vp.getPersona().getNombres(),
                        vp.getPersona().getApellidos(),
                        vp.getFechaAsociacion(),
                        vp.getEstadoConductor()))
                .toList();

        return new VehiculoDetalleCompletoDTO(
                vehiculo.getId(), vehiculo.getTipoVehiculo(), vehiculo.getPlaca(),
                vehiculo.getTipoServicio(), vehiculo.getTipoCombustible(), vehiculo.getCapacidadPasajeros(),
                vehiculo.getColor(), vehiculo.getModelo(), vehiculo.getMarca(), vehiculo.getLinea(),
                documentos, conductores);
    }

    @Override
    public List<ConteoPersonaPorTipoDTO> totalPersonasPorTipo() {
        return personaRepository.contarPersonasPorTipo();
    }
}