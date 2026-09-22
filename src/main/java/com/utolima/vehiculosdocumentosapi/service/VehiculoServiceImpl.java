package com.utolima.vehiculosdocumentosapi.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utolima.vehiculosdocumentosapi.dto.DocumentoAsociadoRequestDTO;
import com.utolima.vehiculosdocumentosapi.dto.VehiculoRequestDTO;
import com.utolima.vehiculosdocumentosapi.exception.RecursoNoEncontradoException;
import com.utolima.vehiculosdocumentosapi.exception.VehiculoSinDocumentoException;
import com.utolima.vehiculosdocumentosapi.model.Documento;
import com.utolima.vehiculosdocumentosapi.model.Vehiculo;
import com.utolima.vehiculosdocumentosapi.model.VehiculoDocumento;
import com.utolima.vehiculosdocumentosapi.model.enums.EstadoDocumento;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoVehiculo;
import com.utolima.vehiculosdocumentosapi.repository.DocumentoRepository;
import com.utolima.vehiculosdocumentosapi.repository.VehiculoDocumentoRepository;
import com.utolima.vehiculosdocumentosapi.repository.VehiculoRepository;

@Service // marca esta clase como un bean de Spring en la capa de servicio (coincide con el @Service del diagrama del PDF)
public class VehiculoServiceImpl implements VehiculoService {

    // "final": una vez asignados en el constructor, no se pueden reasignar -> garantiza que siempre esten inicializados
    private final VehiculoRepository vehiculoRepository;
    private final DocumentoRepository documentoRepository;
    private final VehiculoDocumentoRepository vehiculoDocumentoRepository;

    // constructor injection: Spring detecta este constructor y automaticamente
    // le pasa los 3 repositorios (que ya son @Bean gracias a JpaRepository) sin que tengamos que pedirlos a mano
    public VehiculoServiceImpl(VehiculoRepository vehiculoRepository,
                                DocumentoRepository documentoRepository,
                                VehiculoDocumentoRepository vehiculoDocumentoRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.documentoRepository = documentoRepository;
        this.vehiculoDocumentoRepository = vehiculoDocumentoRepository;
    }

    @Override
    @Transactional // si algo falla a mitad de este metodo, TODO se revierte (ni el vehiculo ni ningun documento quedan guardados a medias)
    public Vehiculo crear(VehiculoRequestDTO dto) {
        // segunda linea de defensa de la regla "minimo un documento": el DTO ya lo valida con @NotEmpty,
        // pero este chequeo protege el metodo aunque lo llamen desde otro lugar sin pasar por esa validacion
        if (dto.getDocumentos() == null || dto.getDocumentos().isEmpty()) {
            throw new VehiculoSinDocumentoException("Un vehículo debe tener al menos un documento asociado.");
        }

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setTipoVehiculo(dto.getTipoVehiculo());
        vehiculo.setPlaca(dto.getPlaca());
        vehiculo.setTipoServicio(dto.getTipoServicio());
        vehiculo.setTipoCombustible(dto.getTipoCombustible());
        vehiculo.setCapacidadPasajeros(dto.getCapacidadPasajeros());
        vehiculo.setColor(dto.getColor());
        vehiculo.setModelo(dto.getModelo());
        vehiculo.setMarca(dto.getMarca());
        vehiculo.setLinea(dto.getLinea());

        Vehiculo vehiculoGuardado = vehiculoRepository.save(vehiculo); // primer INSERT: ya tiene id asignado por AUTO_INCREMENT

        for (DocumentoAsociadoRequestDTO docDto : dto.getDocumentos()) {
            Documento documento = documentoRepository.findById(docDto.getIdDocumento())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe el documento con id " + docDto.getIdDocumento()));

            VehiculoDocumento vd = new VehiculoDocumento();
            vd.setVehiculo(vehiculoGuardado); // @MapsId rellena automaticamente la parte idVehiculo de la llave compuesta
            vd.setDocumento(documento);       // @MapsId rellena automaticamente la parte idDocumento
            vd.setFechaExpedicion(docDto.getFechaExpedicion());
            vd.setFechaVencimiento(docDto.getFechaVencimiento());
            vd.setEstadoDocumento(EstadoDocumento.EN_VERIFICACION); // regla del PDF: SIEMPRE inicia en este estado

            vehiculoDocumentoRepository.save(vd);
        }

        return vehiculoGuardado;
    }

    @Override
    public Vehiculo obtenerPorId(Long id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el vehículo con id " + id));
    }

    @Override
    public List<Vehiculo> listarTodos() {
        return vehiculoRepository.findAll();
    }

    @Override
    @Transactional
    public Vehiculo actualizar(Long id, VehiculoRequestDTO dto) {
        Vehiculo vehiculo = obtenerPorId(id); // reutiliza el metodo de arriba, si no existe ya lanza la excepcion
        vehiculo.setTipoVehiculo(dto.getTipoVehiculo());
        vehiculo.setPlaca(dto.getPlaca());
        vehiculo.setTipoServicio(dto.getTipoServicio());
        vehiculo.setTipoCombustible(dto.getTipoCombustible());
        vehiculo.setCapacidadPasajeros(dto.getCapacidadPasajeros());
        vehiculo.setColor(dto.getColor());
        vehiculo.setModelo(dto.getModelo());
        vehiculo.setMarca(dto.getMarca());
        vehiculo.setLinea(dto.getLinea());
        return vehiculoRepository.save(vehiculo); // save() con un id existente hace UPDATE, no INSERT
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Vehiculo vehiculo = obtenerPorId(id);
        vehiculoRepository.delete(vehiculo);
    }

    @Override
    public Vehiculo buscarPorPlaca(String placa) {
        return vehiculoRepository.findByPlaca(placa)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un vehículo con placa " + placa));
    }

    @Override
    public List<Vehiculo> buscarPorTipoVehiculo(TipoVehiculo tipoVehiculo) {
        return vehiculoRepository.findByTipoVehiculo(tipoVehiculo);
    }

    @Override
    public List<Vehiculo> buscarPorDocumento(Long idDocumento) {
        return vehiculoRepository.findVehiculosPorDocumento(idDocumento);
    }

    @Override
    public List<Vehiculo> buscarPorEstadoDocumento(EstadoDocumento estado) {
        return vehiculoRepository.findVehiculosPorEstadoDocumento(estado);
    }

    @Override
    @Transactional
    public VehiculoDocumento agregarDocumento(Long idVehiculo, DocumentoAsociadoRequestDTO dto) {
        Vehiculo vehiculo = obtenerPorId(idVehiculo);
        Documento documento = documentoRepository.findById(dto.getIdDocumento())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el documento con id " + dto.getIdDocumento()));

        VehiculoDocumento vd = new VehiculoDocumento();
        vd.setVehiculo(vehiculo);
        vd.setDocumento(documento);
        vd.setFechaExpedicion(dto.getFechaExpedicion());
        vd.setFechaVencimiento(dto.getFechaVencimiento());
        vd.setEstadoDocumento(EstadoDocumento.EN_VERIFICACION);

        return vehiculoDocumentoRepository.save(vd);
    }
}