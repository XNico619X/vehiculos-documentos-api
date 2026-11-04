package com.utolima.vehiculosdocumentosapi.dto;
 
import java.time.LocalDate;
 
import com.utolima.vehiculosdocumentosapi.model.enums.EstadoDocumento;
 
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
 
// Cuerpo del PUT que cambia el estado de UN documento de UN vehiculo.
// Es una clase con setters (no un record) para seguir el mismo estilo que CambioEstadoConductorDTO.
@Getter
@Setter
public class CambioEstadoDocumentoDTO {
 
    // Se envia el mismo texto que ya devuelve la API (por ejemplo "Habilitado", "En Verificacion", "Vencido").
    // Jackson lo convierte solo con el @JsonCreator del enum.
    @NotNull(message = "El estado del documento es obligatorio")
    private EstadoDocumento estadoDocumento;
 
    // OPCIONAL. Si el documento ya vencio, hay que mandar aqui una fecha futura para poder habilitarlo.
    private LocalDate fechaVencimiento;
}