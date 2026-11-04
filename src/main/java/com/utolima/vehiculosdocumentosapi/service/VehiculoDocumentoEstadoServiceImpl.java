package com.utolima.vehiculosdocumentosapi.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utolima.vehiculosdocumentosapi.dto.CambioEstadoDocumentoDTO;
import com.utolima.vehiculosdocumentosapi.dto.DocumentoVehiculoEstadoResponseDTO;
import com.utolima.vehiculosdocumentosapi.exception.RecursoNoEncontradoException;
import com.utolima.vehiculosdocumentosapi.exception.ReglaNegocioException;
import com.utolima.vehiculosdocumentosapi.model.VehiculoDocumento;
import com.utolima.vehiculosdocumentosapi.model.enums.EstadoDocumento;
import com.utolima.vehiculosdocumentosapi.repository.VehiculoDocumentoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // inyecta el repositorio por constructor, igual que el resto del proyecto
public class VehiculoDocumentoEstadoServiceImpl implements VehiculoDocumentoEstadoService {

    private final VehiculoDocumentoRepository vehiculoDocumentoRepository;

    @Override
    @Transactional // si algo falla despues de modificar la entidad, no queda nada a medias
    public DocumentoVehiculoEstadoResponseDTO cambiarEstado(Long idVehiculo, Long idDocumento,
                                                            CambioEstadoDocumentoDTO dto) {

        // 1. Buscar la fila de la tabla puente (vehiculo + documento).
        // Se usa findByVehiculo_Id, que ya existe, y se filtra en Java: asi no dependemos de como se construye la llave compuesta.
        VehiculoDocumento vd = vehiculoDocumentoRepository.findByVehiculo_Id(idVehiculo).stream()
                .filter(x -> x.getDocumento() != null && idDocumento.equals(x.getDocumento().getId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El vehiculo " + idVehiculo + " no tiene asociado el documento " + idDocumento));

        // 2. Fecha que quedara: la nueva si la mandan; si no, la que ya tenia.
        LocalDate fechaResultante = (dto.getFechaVencimiento() != null)
                ? dto.getFechaVencimiento()
                : vd.getFechaVencimiento();

        // 3. REGLA: no se puede dejar HABILITADO un documento ya vencido.
        // Si se permitiera, la tarea programada lo volveria a VENCIDO a los pocos minutos y el cambio parecería no funcionar.
        // "Vencido" = fecha anterior a hoy: es el mismo criterio que usa la tarea programada.
        if (dto.getEstadoDocumento() == EstadoDocumento.HABILITADO && fechaResultante.isBefore(LocalDate.now())) {
            throw new ReglaNegocioException(
                    "No se puede habilitar un documento con fecha de vencimiento " + fechaResultante
                            + ". Envie una fechaVencimiento futura junto con el estado.");
        }

        // 4. Aplicar los cambios. Dentro de la transaccion Hibernate detecta el cambio, pero save() lo deja explicito.
        vd.setEstadoDocumento(dto.getEstadoDocumento());
        if (dto.getFechaVencimiento() != null) {
            vd.setFechaVencimiento(dto.getFechaVencimiento());
        }
        vehiculoDocumentoRepository.save(vd);

        return new DocumentoVehiculoEstadoResponseDTO(
                idVehiculo, idDocumento, vd.getEstadoDocumento(), vd.getFechaVencimiento());
    }
}