// ============================================================================
// ui/catalogos.js · TEXTOS LEGIBLES PARA LOS CÓDIGOS DEL BACKEND
// ============================================================================
// El backend responde con CÓDIGOS cortos (los de los enums de Java con @JsonValue):
//   tipoServicio "Pu"/"Pr" · tipoPersona "C"/"A" · estadoConductor "PO"/"EA"/"RO"
//   estadoDocumento "Habilitado"/"Vencido"/"En Verificacion" · combustible "Disel" (sin tilde, así está en la BD)
// El usuario NO debe ver eso: aquí se traducen a texto bonito y a un color de etiqueta.
//
// Si el backend algún día manda un valor que no está aquí, se muestra tal cual llegó
// (mejor ver el código crudo que una celda vacía o un error).

import { etiqueta } from "./util.js";

export const TIPO_VEHICULO = { AUTOMOVIL: "Automóvil", MOTOCICLETA: "Motocicleta" };
export const TIPO_SERVICIO = { Pu: "Público", Pr: "Privado", PUBLICO: "Público", PRIVADO: "Privado" };
export const TIPO_COMBUSTIBLE = { Gasolina: "Gasolina", Gas: "Gas", Disel: "Diésel", DIESEL: "Diésel", GASOLINA: "Gasolina", GAS: "Gas" };
export const TIPO_PERSONA = { C: "Conductor", A: "Administrativo", CONDUCTOR: "Conductor", ADMINISTRATIVO: "Administrativo" };

// Cada estado lleva su texto y el TIPO de etiqueta (color): ok = verde, aviso = amarillo, error = rojo
export const ESTADO_DOCUMENTO = {
    "Habilitado": { texto: "Habilitado", tipo: "ok" },
    "En Verificacion": { texto: "En verificación", tipo: "aviso" },
    "Vencido": { texto: "Vencido", tipo: "error" },
};

export const ESTADO_CONDUCTOR = {
    PO: { texto: "Puede operar", tipo: "ok" },
    EA: { texto: "En espera de aprobación", tipo: "aviso" },
    RO: { texto: "Restringido", tipo: "error" },
    PUEDE_OPERAR: { texto: "Puede operar", tipo: "ok" },
    ESPERA_APROBACION: { texto: "En espera de aprobación", tipo: "aviso" },
    RESTRINGIDO: { texto: "Restringido", tipo: "error" },
};

// MotivoRestriccion NO tiene @JsonValue: llega con el nombre de la constante de Java
export const MOTIVO_RESTRICCION = {
    VEHICULO_NO_HABILITADO: "Vehículo no habilitado",
    CONDUCTOR_RESTRINGIDO: "Conductor restringido",
    AMBOS: "Vehículo y conductor",
};

// Traduce un código con un catálogo simple { codigo: "Texto" }. Si no existe, devuelve el código crudo.
export function textoDe(catalogo, codigo) {
    return catalogo[codigo] ?? (codigo ?? "—");
}

// Etiquetas de color listas para poner en una celda o tarjeta
export function chipEstadoDocumento(estado) {
    const e = ESTADO_DOCUMENTO[estado];
    return etiqueta(e?.texto ?? estado ?? "—", e?.tipo ?? "");
}

export function chipEstadoConductor(estado) {
    const e = ESTADO_CONDUCTOR[estado];
    return etiqueta(e?.texto ?? estado ?? "—", e?.tipo ?? "");
}

// 12 -> "Vence en 12 días" · 0 -> "Vence hoy" · -3 -> "Venció hace 3 días"
export function textoDias(dias) {
    if (dias === null || dias === undefined) return "";
    if (dias === 0) return "Vence hoy";
    const n = Math.abs(dias);
    const unidad = n === 1 ? "día" : "días";
    return dias > 0 ? `Vence en ${n} ${unidad}` : `Venció hace ${n} ${unidad}`;
}