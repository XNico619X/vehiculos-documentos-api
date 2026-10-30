package com.utolima.vehiculosdocumentosapi.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utolima.vehiculosdocumentosapi.dto.CargueResponseDTO;
import com.utolima.vehiculosdocumentosapi.exception.RecursoNoEncontradoException;
import com.utolima.vehiculosdocumentosapi.mapper.TrayectoTmpMapper;
import com.utolima.vehiculosdocumentosapi.model.TrayectoTmp;
import com.utolima.vehiculosdocumentosapi.repository.TrayectoTmpRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // inyecta el repositorio por constructor, igual que el resto del proyecto
public class CargueConsultaServiceImpl implements CargueConsultaService {

    private final TrayectoTmpRepository trayectoTmpRepository;

    // readOnly = true: solo lee, no modifica nada en la base.
    @Override
    @Transactional(readOnly = true)
    public CargueResponseDTO consultarCargue(Long idCargue) {
        // Todas las filas del cargue, ya ordenadas por idTrayectoTmp (lo hace el nombre del método del repositorio)
        List<TrayectoTmp> filas = trayectoTmpRepository.findByIdCargueOrderByIdTrayectoTmpAsc(idCargue);

        // Si no hay ninguna fila, ese cargue no existe -> el manejador global lo convierte en HTTP 404
        if (filas.isEmpty()) {
            throw new RecursoNoEncontradoException("No existe un cargue con id " + idCargue);
        }

        // RESUMEN: siempre los cuatro estados, en este orden, con 0 si no hay ninguna fila.
        // LinkedHashMap conserva el orden en que se insertan las claves.
        Map<String, Long> resumen = new LinkedHashMap<>();
        resumen.put("CARGADO", 0L);
        resumen.put("VALIDADO", 0L);
        resumen.put("PROCESADO", 0L);
        resumen.put("ERROR", 0L);

        // Contamos en Java desde la lista (no con otra consulta a la base de datos)
        for (TrayectoTmp fila : filas) {
            if (resumen.containsKey(fila.getEstado())) { // el CHECK de la tabla garantiza que sea uno de los cuatro
                resumen.merge(fila.getEstado(), 1L, Long::sum);
            }
        }

        // Entidades -> DTOs: nunca devolvemos la entidad
        return new CargueResponseDTO(idCargue, filas.size(), resumen, TrayectoTmpMapper.toResponseDTOList(filas));
    }
}