package com.utolima.vehiculosdocumentosapi.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// Le dice a Hibernate "cada vez que veas un campo de tipo TipoServicio en cualquier entidad, usa este converter automáticamente" din tener que ponerlo en cada entity
@Converter(autoApply = true)

// basicamente es para que provee los que cada interfaz exige sea codigo o base de datos
public class TipoServicioConverter implements AttributeConverter<TipoServicio, String> {

	//'override' anotación puramente de seguridad y claridad para el compilador.
	//hibernate llama a este metodo cuando va guardar recibe el valor java y debe devolver el string que se va a escribir en la columna (base de datos) por eso .getCodigo() que devuleve 'Pu o Pr'
    @Override
    public String convertToDatabaseColumn(TipoServicio tipoServicio) {
        return tipoServicio == null ? null : tipoServicio.getCodigo();
    }

    
    // hibernate llama este metodo cuando va a leer recibe el string que trae la fila de la base y devuelve el objeto java correspondiente. fromCodigo() busqueda inversa
    @Override
    public TipoServicio convertToEntityAttribute(String codigo) {
        return codigo == null ? null : TipoServicio.fromCodigo(codigo); //proteccion contra nulos
    }
}