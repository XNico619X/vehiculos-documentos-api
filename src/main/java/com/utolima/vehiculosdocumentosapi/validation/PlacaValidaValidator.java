package com.utolima.vehiculosdocumentosapi.validation;

import com.utolima.vehiculosdocumentosapi.dto.VehiculoRequestDTO;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoVehiculo;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// ConstraintValidator<LaAnotacion, TipoDeObjetoQueValida>
public class PlacaValidaValidator implements ConstraintValidator<PlacaValida, VehiculoRequestDTO> {

    // regex: 3 letras + 3 numeros (automovil)
    private static final String REGEX_AUTOMOVIL = "^[A-Za-z]{3}[0-9]{3}$";
    // regex: 3 letras + 2 numeros + 1 letra (motocicleta)
    private static final String REGEX_MOTOCICLETA = "^[A-Za-z]{3}[0-9]{2}[A-Za-z]$";

    @Override
    public boolean isValid(VehiculoRequestDTO dto, ConstraintValidatorContext context) {
        if (dto.getPlaca() == null || dto.getTipoVehiculo() == null) {
            return true; // si vienen null, dejamos que @NotNull de cada campo individual reporte ESE error
        }

        String placa = dto.getPlaca();
        TipoVehiculo tipo = dto.getTipoVehiculo();

        boolean valido = switch (tipo) {
            case AUTOMOVIL -> placa.matches(REGEX_AUTOMOVIL);
            case MOTOCICLETA -> placa.matches(REGEX_MOTOCICLETA);
        };

        if (!valido) {
            context.disableDefaultConstraintViolation(); // apaga el mensaje generico de "message" de la anotacion
            String formatoEsperado = tipo == TipoVehiculo.AUTOMOVIL
                    ? "3 letras seguidas de 3 números (ej. ABC123)"
                    : "3 letras, 2 números y una letra final (ej. ABC12A)";
            context.buildConstraintViolationWithTemplate(
                    "Formato de placa inválido para " + tipo + ". Se esperaba: " + formatoEsperado)
                    .addPropertyNode("placa") // asocia el error especificamente al campo "placa" en la respuesta JSON
                    .addConstraintViolation();
        }

        return valido; // Bean Validation usa este booleano para decidir si el objeto pasa o no
    }
}