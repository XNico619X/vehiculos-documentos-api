package com.utolima.vehiculosdocumentosapi.controller;

import java.util.List;

import com.utolima.vehiculosdocumentosapi.dto.VehiculoResponseDTO;
import com.utolima.vehiculosdocumentosapi.mapper.VehiculoMapper;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utolima.vehiculosdocumentosapi.dto.DocumentoAsociadoRequestDTO;
import com.utolima.vehiculosdocumentosapi.dto.VehiculoRequestDTO;
import com.utolima.vehiculosdocumentosapi.model.Vehiculo;
import com.utolima.vehiculosdocumentosapi.model.VehiculoDocumento;
import com.utolima.vehiculosdocumentosapi.model.enums.EstadoDocumento;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoVehiculo;
import com.utolima.vehiculosdocumentosapi.service.VehiculoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController // combina @Controller + @ResponseBody: cada metodo devuelve JSON directo, no vistas HTML
@RequestMapping("/api/vehiculos") // prefijo comun para TODAS las rutas de esta clase
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoService vehiculoService;
// se cambio el tipo de retorno de los metodos que devuelven vehiculo por VehiculoResponseDTO
    @PostMapping // responde a POST /api/vehiculos
    public ResponseEntity<VehiculoResponseDTO> crear(@Valid @RequestBody VehiculoRequestDTO dto) {
        Vehiculo creado = vehiculoService.crear(dto);
        return new ResponseEntity<>(VehiculoMapper.toResponseDTO(creado), HttpStatus.CREATED);
    }


    @GetMapping // GET /api/vehiculos
    public ResponseEntity<List<VehiculoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(VehiculoMapper.toResponseDTOList(vehiculoService.listarTodos()));
    }

    @GetMapping("/{id}") // GET /api/vehiculos/5
    public ResponseEntity<VehiculoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(VehiculoMapper.toResponseDTO(vehiculoService.obtenerPorId(id)));
    }

    @PutMapping("/{id}") // PUT /api/vehiculos/5
    public ResponseEntity<VehiculoResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody VehiculoRequestDTO dto) {
        return ResponseEntity.ok(VehiculoMapper.toResponseDTO(vehiculoService.actualizar(id, dto)));
    }

    @DeleteMapping("/{id}") // DELETE /api/vehiculos/5
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        vehiculoService.eliminar(id);
        return ResponseEntity.noContent().build(); // 204: exito, pero sin cuerpo de respuesta (convencion para DELETE)
    }

    @GetMapping("/placa/{placa}") // GET /api/vehiculos/placa/ABC123
    public ResponseEntity<VehiculoResponseDTO> buscarPorPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(VehiculoMapper.toResponseDTO(vehiculoService.buscarPorPlaca(placa)));
    }

    @GetMapping("/tipo/{tipoVehiculo}") // GET /api/vehiculos/tipo/AUTOMOVIL
    public ResponseEntity<List<VehiculoResponseDTO>> buscarPorTipoVehiculo(@PathVariable String tipoVehiculo) {
        TipoVehiculo tipo = TipoVehiculo.fromCodigo(tipoVehiculo);
        return ResponseEntity.ok(VehiculoMapper.toResponseDTOList(vehiculoService.buscarPorTipoVehiculo(tipo)));
    }


    @GetMapping("/documento/{idDocumento}") // GET /api/vehiculos/documento/3
    public ResponseEntity<List<VehiculoResponseDTO>> buscarPorDocumento(@PathVariable Long idDocumento) {
        return ResponseEntity.ok(VehiculoMapper.toResponseDTOList(vehiculoService.buscarPorDocumento(idDocumento)));
    }

    @GetMapping("/estado-documento/{estado}") // GET /api/vehiculos/estado-documento/Habilitado
    public ResponseEntity<List<VehiculoResponseDTO>> buscarPorEstadoDocumento(@PathVariable String estado) {
        EstadoDocumento estadoDocumento = EstadoDocumento.fromCodigo(estado);
        return ResponseEntity.ok(VehiculoMapper.toResponseDTOList(vehiculoService.buscarPorEstadoDocumento(estadoDocumento)));
    }

    @PostMapping("/{id}/documentos") // POST /api/vehiculos/5/documentos
    public ResponseEntity<VehiculoDocumento> agregarDocumento(@PathVariable Long id,
                                                               @Valid @RequestBody DocumentoAsociadoRequestDTO dto) {
        VehiculoDocumento creado = vehiculoService.agregarDocumento(id, dto);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }
}