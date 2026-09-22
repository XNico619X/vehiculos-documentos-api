package com.utolima.vehiculosdocumentosapi.mapper;

import java.util.List;

import com.utolima.vehiculosdocumentosapi.dto.DocumentoAsociadoResponseDTO;
import com.utolima.vehiculosdocumentosapi.dto.VehiculoResponseDTO;
import com.utolima.vehiculosdocumentosapi.model.Vehiculo;
import com.utolima.vehiculosdocumentosapi.model.VehiculoDocumento;

public class VehiculoMapper { // clase utilitaria: solo metodos estaticos, no se instancia

    public static VehiculoResponseDTO toResponseDTO(Vehiculo vehiculo) {
        List<DocumentoAsociadoResponseDTO> documentos = vehiculo.getDocumentosAsociados().stream()
                .map(VehiculoMapper::mapDocumentoAsociado) // por cada VehiculoDocumento, lo convierte a su DTO
                .toList();

        return new VehiculoResponseDTO(
                vehiculo.getId(),
                vehiculo.getTipoVehiculo(),
                vehiculo.getPlaca(),
                vehiculo.getTipoServicio(),
                vehiculo.getTipoCombustible(),
                vehiculo.getCapacidadPasajeros(),
                vehiculo.getColor(),
                vehiculo.getModelo(),
                vehiculo.getMarca(),
                vehiculo.getLinea(),
                documentos
        );
    }

    public static List<VehiculoResponseDTO> toResponseDTOList(List<Vehiculo> vehiculos) {
        return vehiculos.stream().map(VehiculoMapper::toResponseDTO).toList(); // reutiliza el metodo de arriba para cada vehiculo de la lista
    }

    private static DocumentoAsociadoResponseDTO mapDocumentoAsociado(VehiculoDocumento vd) {
        return new DocumentoAsociadoResponseDTO(
                vd.getDocumento().getId(),
                vd.getDocumento().getCodigoDocumento(),
                vd.getDocumento().getNombreDocumento(),
                vd.getFechaExpedicion(),
                vd.getFechaVencimiento(),
                vd.getEstadoDocumento()
        );
    }
}