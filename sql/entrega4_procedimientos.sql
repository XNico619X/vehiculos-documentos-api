-- =====================================================================================================
-- ENTREGA 4 - Procedimientos almacenados del cargue de trayectos desde Excel
--
--   sp_cargue      PASO 1: guarda UNA fila del Excel, tal cual, en trayecto_tmp (estado CARGADO)
--   sp_validacion  PASO 2: valida las filas CARGADO de un cargue y las marca VALIDADO o ERROR
--   sp_procesado   PASO 3: traslada a trayecto las rutas completas y validadas (estado PROCESADO)
--
-- Orden de uso:
--   CALL sp_cargue(...)            -- una vez por cada fila del Excel
--   CALL sp_validacion(id_cargue)
--   CALL sp_procesado(id_cargue)
--
-- Requisitos previos: la tabla trayecto_tmp (sql/entrega4_trayecto_tmp.sql) y las tablas de las entregas 1 a 3.
--
-- Como ejecutarlo en DBeaver: seleccione cada procedimiento (desde su DROP hasta su END;) y pulse Ctrl+Enter.
-- Se puede ejecutar de nuevo cuando quiera: cada procedimiento empieza con DROP PROCEDURE IF EXISTS.
-- La aplicacion Spring NO crea estos procedimientos (ddl-auto=validate solo revisa tablas):
-- deben existir en cada base de datos donde se use el servicio de cargue.
--
-- Reglas comunes a los tres:
--   * Ninguno devuelve resultados (sin SELECT final) ni confirma transacciones (sin COMMIT ni
--     START TRANSACTION): el servicio de Java los ejecuta dentro de UNA sola transaccion.
--   * Las tablas TEMPORARY no confirman la transaccion, por eso se pueden usar aqui.
--   * Los comentarios y los mensajes van sin tildes a proposito, para evitar problemas de codificacion.
--   * Las reglas de validacion replican las de TrayectoServiceImpl.crearRuta (Java): si cambia una,
--     hay que cambiar la otra.
-- =====================================================================================================


-- =====================================================================================================
-- sp_cargue: PASO 1 del cargue. Guarda UNA fila del Excel, tal cual, en trayecto_tmp.
-- No valida ni modifica ningun dato: la validacion es trabajo de sp_validacion.
-- =====================================================================================================
DROP PROCEDURE IF EXISTS sp_cargue;

CREATE PROCEDURE sp_cargue(
    IN p_id_cargue     BIGINT,        -- identificador del cargue (yyyyMMddHHmm); lo genera el servicio Java
    IN p_id_persona    VARCHAR(50),   -- los 8 campos siguientes llegan como TEXTO, igual que en el Excel
    IN p_id_vehiculo   VARCHAR(50),
    IN p_codigo_ruta   VARCHAR(100),
    IN p_ubicacion     VARCHAR(500),
    IN p_orden_parada  VARCHAR(50),
    IN p_latitud       VARCHAR(50),
    IN p_longitud      VARCHAR(50),
    IN p_login_usuario VARCHAR(100)
)
BEGIN
    -- Sin TRIM, sin UPPER, sin conversiones: lo que llega es lo que se guarda.
    -- Un NULL (celda vacia en el Excel) se guarda como NULL.
    INSERT INTO trayecto_tmp
        (id_cargue, id_persona, id_vehiculo, codigo_ruta, ubicacion,
         orden_parada, latitud, longitud, login_usuario, estado)
    VALUES
        (p_id_cargue, p_id_persona, p_id_vehiculo, p_codigo_ruta, p_ubicacion,
         p_orden_parada, p_latitud, p_longitud, p_login_usuario, 'CARGADO');

    -- IMPORTANTE: no hay SELECT final, ni COMMIT, ni START TRANSACTION.
    -- El servicio ejecuta todo el cargue en UNA sola transaccion; un COMMIT aqui la rompe.
END;


-- =====================================================================================================
-- sp_validacion: PASO 2 del cargue. Revisa las filas CARGADO de un cargue y las marca VALIDADO o ERROR.
-- ESTRATEGIA: primero se RECOLECTAN todos los errores en una tabla temporal (una fila por cada error)
-- y solo AL FINAL se decide el estado de cada fila. Asi una fila con varios problemas queda con TODOS
-- sus mensajes juntos (observacion acumulativa).
-- Solo toca filas CARGADO del cargue: ejecutarlo dos veces no duplica mensajes ni cambia nada.
-- =====================================================================================================
DROP PROCEDURE IF EXISTS sp_validacion;

CREATE PROCEDURE sp_validacion(
    IN p_id_cargue BIGINT          -- el cargue a validar (yyyyMMddHHmm)
)
BEGIN
    -- ===================== PREPARACION =====================
    -- Tablas TEMPORARY: viven solo en esta conexion y NO confirman (COMMIT) la transaccion del servicio.
    -- Se borran primero por si una ejecucion anterior fallo a medias y la conexion se reutilizo.
    DROP TEMPORARY TABLE IF EXISTS tmp_val_errores;
    DROP TEMPORARY TABLE IF EXISTS tmp_val_rutas;

    CREATE TEMPORARY TABLE tmp_val_errores (
        seq             INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,  -- orden de aparicion (ordena los mensajes)
        id_trayecto_tmp BIGINT       NOT NULL,                              -- la fila con el problema
        mensaje         VARCHAR(200) NOT NULL                               -- el problema, en texto
    );
    CREATE TEMPORARY TABLE tmp_val_rutas (
        codigo_ruta VARCHAR(100) NOT NULL    -- codigos de ruta con al menos una fila con error
    );

    -- ===================== BLOQUE 1: reglas de cada fila por separado =====================
    -- Cada INSERT revisa UN campo. El CASE devuelve el mensaje del PRIMER problema que encuentre
    -- (o NULL si el campo esta bien) y el WHERE externo deja solo las filas con mensaje.
    -- CASE se evalua en orden y se detiene: el CAST solo corre cuando REGEXP ya confirmo el formato
    -- (un CAST sobre texto como 'abc' provocaria un error en modo estricto).
    -- TRIM en todos los casos: es el mismo valor que sp_procesado inserta en trayecto.

    -- id_persona: obligatorio y entero (hasta 18 digitos: cabe en BIGINT)
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT x.id_trayecto_tmp, x.mensaje
      FROM (SELECT t.id_trayecto_tmp,
                   CASE WHEN t.id_persona IS NULL OR TRIM(t.id_persona) = ''
                             THEN 'El id_persona es obligatorio'
                        WHEN TRIM(t.id_persona) NOT REGEXP '^[0-9]{1,18}$'
                             THEN 'El id_persona debe ser un numero entero positivo (maximo 18 digitos)'
                   END AS mensaje
              FROM trayecto_tmp t
             WHERE t.id_cargue = p_id_cargue AND t.estado = 'CARGADO') x
     WHERE x.mensaje IS NOT NULL;

    -- id_vehiculo: obligatorio y entero
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT x.id_trayecto_tmp, x.mensaje
      FROM (SELECT t.id_trayecto_tmp,
                   CASE WHEN t.id_vehiculo IS NULL OR TRIM(t.id_vehiculo) = ''
                             THEN 'El id_vehiculo es obligatorio'
                        WHEN TRIM(t.id_vehiculo) NOT REGEXP '^[0-9]{1,18}$'
                             THEN 'El id_vehiculo debe ser un numero entero positivo (maximo 18 digitos)'
                   END AS mensaje
              FROM trayecto_tmp t
             WHERE t.id_cargue = p_id_cargue AND t.estado = 'CARGADO') x
     WHERE x.mensaje IS NOT NULL;

    -- codigo_ruta: obligatorio, maximo 50 caracteres (el ancho de la columna en trayecto)
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT x.id_trayecto_tmp, x.mensaje
      FROM (SELECT t.id_trayecto_tmp,
                   CASE WHEN t.codigo_ruta IS NULL OR TRIM(t.codigo_ruta) = ''
                             THEN 'El codigo_ruta es obligatorio'
                        WHEN CHAR_LENGTH(TRIM(t.codigo_ruta)) > 50
                             THEN 'El codigo_ruta supera los 50 caracteres'
                   END AS mensaje
              FROM trayecto_tmp t
             WHERE t.id_cargue = p_id_cargue AND t.estado = 'CARGADO') x
     WHERE x.mensaje IS NOT NULL;

    -- ubicacion: obligatoria, maximo 255 caracteres
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT x.id_trayecto_tmp, x.mensaje
      FROM (SELECT t.id_trayecto_tmp,
                   CASE WHEN t.ubicacion IS NULL OR TRIM(t.ubicacion) = ''
                             THEN 'La ubicacion es obligatoria'
                        WHEN CHAR_LENGTH(TRIM(t.ubicacion)) > 255
                             THEN 'La ubicacion supera los 255 caracteres'
                   END AS mensaje
              FROM trayecto_tmp t
             WHERE t.id_cargue = p_id_cargue AND t.estado = 'CARGADO') x
     WHERE x.mensaje IS NOT NULL;

    -- orden_parada: obligatorio y entero. Aqui solo se revisa el formato; que este dentro de 0..n-1
    -- depende de cuantas paradas tiene la ruta y se revisa en el BLOQUE 2.
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT x.id_trayecto_tmp, x.mensaje
      FROM (SELECT t.id_trayecto_tmp,
                   CASE WHEN t.orden_parada IS NULL OR TRIM(t.orden_parada) = ''
                             THEN 'El orden_parada es obligatorio'
                        WHEN TRIM(t.orden_parada) NOT REGEXP '^[0-9]{1,3}$'
                             THEN 'El orden_parada debe ser un numero entero entre 0 y 999'
                   END AS mensaje
              FROM trayecto_tmp t
             WHERE t.id_cargue = p_id_cargue AND t.estado = 'CARGADO') x
     WHERE x.mensaje IS NOT NULL;

    -- latitud: OPCIONAL (vacia es valida: la tarea de Google Maps la completa). Si viene: numero con
    -- punto decimal, maximo 8 decimales (el ancho de DECIMAL(10,8)) y entre -90 y 90.
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT x.id_trayecto_tmp, x.mensaje
      FROM (SELECT t.id_trayecto_tmp,
                   CASE WHEN t.latitud IS NULL OR TRIM(t.latitud) = ''
                             THEN NULL
                        WHEN TRIM(t.latitud) NOT REGEXP '^-?[0-9]{1,3}([.][0-9]{1,8})?$'
                             THEN 'La latitud debe ser un numero con punto decimal (maximo 8 decimales), por ejemplo 4.4417'
                        WHEN CAST(TRIM(t.latitud) AS DECIMAL(12,8)) NOT BETWEEN -90 AND 90
                             THEN 'La latitud debe estar entre -90 y 90'
                   END AS mensaje
              FROM trayecto_tmp t
             WHERE t.id_cargue = p_id_cargue AND t.estado = 'CARGADO') x
     WHERE x.mensaje IS NOT NULL;

    -- longitud: igual que la latitud, pero entre -180 y 180
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT x.id_trayecto_tmp, x.mensaje
      FROM (SELECT t.id_trayecto_tmp,
                   CASE WHEN t.longitud IS NULL OR TRIM(t.longitud) = ''
                             THEN NULL
                        WHEN TRIM(t.longitud) NOT REGEXP '^-?[0-9]{1,3}([.][0-9]{1,8})?$'
                             THEN 'La longitud debe ser un numero con punto decimal (maximo 8 decimales), por ejemplo -75.2455'
                        WHEN CAST(TRIM(t.longitud) AS DECIMAL(12,8)) NOT BETWEEN -180 AND 180
                             THEN 'La longitud debe estar entre -180 y 180'
                   END AS mensaje
              FROM trayecto_tmp t
             WHERE t.id_cargue = p_id_cargue AND t.estado = 'CARGADO') x
     WHERE x.mensaje IS NOT NULL;

    -- login_usuario: obligatorio, maximo 50 caracteres
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT x.id_trayecto_tmp, x.mensaje
      FROM (SELECT t.id_trayecto_tmp,
                   CASE WHEN t.login_usuario IS NULL OR TRIM(t.login_usuario) = ''
                             THEN 'El login_usuario es obligatorio'
                        WHEN CHAR_LENGTH(TRIM(t.login_usuario)) > 50
                             THEN 'El login_usuario supera los 50 caracteres'
                   END AS mensaje
              FROM trayecto_tmp t
             WHERE t.id_cargue = p_id_cargue AND t.estado = 'CARGADO') x
     WHERE x.mensaje IS NOT NULL;

    -- ===================== BLOQUE 2: reglas de cada RUTA =====================
    -- Una "ruta" = todas las filas del cargue con el mismo codigo_ruta (comparado con TRIM).
    -- Estas reglas miran el CONJUNTO de paradas, no una fila sola, por eso usan GROUP BY.
    -- Participan todas las filas CARGADO con codigo_ruta lleno, incluso las que ya tienen un error del
    -- BLOQUE 1: la cantidad de paradas es la que el usuario puso en el Excel.
    -- Solo se comparan como numeros los valores con formato valido: el CASE (o el REGEXP del WHERE) evita
    -- hacer CAST sobre texto como 'abc'. Esos valores invalidos ya los reporto el BLOQUE 1.
    -- Se comparan NUMERICAMENTE: '01' y '1' son el mismo orden, '007' y '7' la misma persona. Asi lo ve
    -- sp_procesado (usa CAST) y asi lo veria la llave unica de trayecto (codigo_ruta, orden_parada).
    -- Los limites 2 y 7 (parada inicial + final, maximo 5 intermedias) son los de crearRuta (Entrega 3).
    -- Aqui no se pueden usar DECLARE (solo van al inicio del procedimiento), por eso van escritos.
    -- Los mensajes con datos usan LEFT(..., 200): la columna mensaje mide 200.

    -- R1: la ruta debe tener entre 2 y 7 paradas. El mensaje va en TODAS sus filas.
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT t.id_trayecto_tmp,
           CONCAT('La ruta tiene ', r.total, ' parada(s); debe tener entre 2 y 7')
      FROM trayecto_tmp t
      JOIN (SELECT TRIM(codigo_ruta) AS ruta, COUNT(*) AS total
              FROM trayecto_tmp
             WHERE id_cargue = p_id_cargue AND estado = 'CARGADO'
               AND codigo_ruta IS NOT NULL AND TRIM(codigo_ruta) <> ''
             GROUP BY TRIM(codigo_ruta)) r
        ON r.ruta = TRIM(t.codigo_ruta)
     WHERE t.id_cargue = p_id_cargue AND t.estado = 'CARGADO'
       AND (r.total < 2 OR r.total > 7);

    -- R2: todas las paradas de la ruta deben tener el MISMO conductor.
    -- La subconsulta interna saca los valores DISTINTOS de id_persona por ruta (ya como numero);
    -- si hay mas de uno, el mensaje lista cuales son, y va en todas las filas de la ruta.
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT t.id_trayecto_tmp,
           LEFT(CONCAT('La ruta mezcla conductores distintos (id_persona: ', r.valores, ')'), 200)
      FROM trayecto_tmp t
      JOIN (SELECT d.ruta, COUNT(*) AS distintos,
                   GROUP_CONCAT(d.valor ORDER BY d.valor SEPARATOR ', ') AS valores
              FROM (SELECT DISTINCT TRIM(codigo_ruta) AS ruta,
                                    CAST(TRIM(id_persona) AS UNSIGNED) AS valor
                      FROM trayecto_tmp
                     WHERE id_cargue = p_id_cargue AND estado = 'CARGADO'
                       AND codigo_ruta IS NOT NULL AND TRIM(codigo_ruta) <> ''
                       AND TRIM(id_persona) REGEXP '^[0-9]{1,18}$') d
             GROUP BY d.ruta) r
        ON r.ruta = TRIM(t.codigo_ruta)
     WHERE t.id_cargue = p_id_cargue AND t.estado = 'CARGADO'
       AND r.distintos > 1;

    -- R3: igual que R2, pero con el vehiculo (id_vehiculo).
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT t.id_trayecto_tmp,
           LEFT(CONCAT('La ruta mezcla vehiculos distintos (id_vehiculo: ', r.valores, ')'), 200)
      FROM trayecto_tmp t
      JOIN (SELECT d.ruta, COUNT(*) AS distintos,
                   GROUP_CONCAT(d.valor ORDER BY d.valor SEPARATOR ', ') AS valores
              FROM (SELECT DISTINCT TRIM(codigo_ruta) AS ruta,
                                    CAST(TRIM(id_vehiculo) AS UNSIGNED) AS valor
                      FROM trayecto_tmp
                     WHERE id_cargue = p_id_cargue AND estado = 'CARGADO'
                       AND codigo_ruta IS NOT NULL AND TRIM(codigo_ruta) <> ''
                       AND TRIM(id_vehiculo) REGEXP '^[0-9]{1,18}$') d
             GROUP BY d.ruta) r
        ON r.ruta = TRIM(t.codigo_ruta)
     WHERE t.id_cargue = p_id_cargue AND t.estado = 'CARGADO'
       AND r.distintos > 1;

    -- R4: orden_parada sin repetir dentro de la ruta. HAVING COUNT(*) > 1 deja solo los pares
    -- (ruta, orden) que aparecen mas de una vez, y el mensaje va SOLO en las filas repetidas.
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT t.id_trayecto_tmp,
           CONCAT('El orden_parada ', d.orden_n, ' esta repetido en la ruta')
      FROM trayecto_tmp t
      JOIN (SELECT TRIM(codigo_ruta) AS ruta,
                   CAST(TRIM(orden_parada) AS UNSIGNED) AS orden_n
              FROM trayecto_tmp
             WHERE id_cargue = p_id_cargue AND estado = 'CARGADO'
               AND codigo_ruta IS NOT NULL AND TRIM(codigo_ruta) <> ''
               AND TRIM(orden_parada) REGEXP '^[0-9]{1,3}$'
             GROUP BY TRIM(codigo_ruta), CAST(TRIM(orden_parada) AS UNSIGNED)
            HAVING COUNT(*) > 1) d
        ON d.ruta = TRIM(t.codigo_ruta)
       AND d.orden_n = CASE WHEN TRIM(t.orden_parada) REGEXP '^[0-9]{1,3}$'
                            THEN CAST(TRIM(t.orden_parada) AS UNSIGNED) END
     WHERE t.id_cargue = p_id_cargue AND t.estado = 'CARGADO';

    -- R5: orden_parada dentro de 0 .. n-1, con n = cantidad de paradas de la ruta.
    -- Con R4 (sin repetir) garantiza el orden exacto 0,1,2,...,n-1 sin huecos.
    -- El CASE hace de guardia: el CAST solo se evalua si el texto ya es un numero.
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT t.id_trayecto_tmp,
           CONCAT('El orden_parada ', CAST(TRIM(t.orden_parada) AS UNSIGNED),
                  ' esta fuera del rango 0 a ', r.total - 1,
                  ' (la ruta tiene ', r.total, ' parada(s))')
      FROM trayecto_tmp t
      JOIN (SELECT TRIM(codigo_ruta) AS ruta, COUNT(*) AS total
              FROM trayecto_tmp
             WHERE id_cargue = p_id_cargue AND estado = 'CARGADO'
               AND codigo_ruta IS NOT NULL AND TRIM(codigo_ruta) <> ''
             GROUP BY TRIM(codigo_ruta)) r
        ON r.ruta = TRIM(t.codigo_ruta)
     WHERE t.id_cargue = p_id_cargue AND t.estado = 'CARGADO'
       AND CASE WHEN TRIM(t.orden_parada) REGEXP '^[0-9]{1,3}$'
                THEN CAST(TRIM(t.orden_parada) AS UNSIGNED) >= r.total END;

    -- ===================== BLOQUE 3: reglas contra la base de datos =====================
    -- Estas reglas consultan las tablas reales (persona, vehiculos, vehiculo_persona, vehiculo_documento,
    -- trayecto) y replican las de crearRuta en Java (Entrega 3).
    -- Para no repetir conversiones en cada regla, primero se arma UNA tabla temporal con los ids ya como
    -- NUMERO (o NULL si el texto no tenia formato valido: eso ya lo reporto el BLOQUE 1).
    -- El CASE con REGEXP evita hacer CAST sobre texto como 'abc'.
    -- Se borra al final de este bloque: las conexiones se reutilizan y no se deja nada vivo.
    -- Una tabla TEMPORARY no puede aparecer dos veces en la misma consulta, por eso cada regla la usa una vez.

    DROP TEMPORARY TABLE IF EXISTS tmp_val_filas;
    CREATE TEMPORARY TABLE tmp_val_filas (
        id_trayecto_tmp BIGINT       NOT NULL PRIMARY KEY,
        persona_n       BIGINT       NULL,   -- id_persona como numero (NULL si no era un entero valido)
        vehiculo_n      BIGINT       NULL,   -- id_vehiculo como numero
        ruta            VARCHAR(100) NULL    -- codigo_ruta recortado (NULL si venia vacio)
    );

    INSERT INTO tmp_val_filas (id_trayecto_tmp, persona_n, vehiculo_n, ruta)
    SELECT t.id_trayecto_tmp,
           CASE WHEN TRIM(t.id_persona)  REGEXP '^[0-9]{1,18}$' THEN CAST(TRIM(t.id_persona)  AS UNSIGNED) END,
           CASE WHEN TRIM(t.id_vehiculo) REGEXP '^[0-9]{1,18}$' THEN CAST(TRIM(t.id_vehiculo) AS UNSIGNED) END,
           NULLIF(TRIM(t.codigo_ruta), '')
      FROM trayecto_tmp t
     WHERE t.id_cargue = p_id_cargue AND t.estado = 'CARGADO';

    -- R6: el conductor debe existir y ser tipo CONDUCTOR ('C').
    -- LEFT JOIN + "IS NULL" es la forma de preguntar "no existe": si no hay persona, p.id_persona sale NULL.
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT f.id_trayecto_tmp,
           CASE WHEN p.id_persona IS NULL
                     THEN CONCAT('El conductor (id_persona ', f.persona_n, ') no existe')
                ELSE CONCAT('La persona ', f.persona_n, ' no es de tipo CONDUCTOR')
           END
      FROM tmp_val_filas f
      LEFT JOIN persona p ON p.id_persona = f.persona_n
     WHERE f.persona_n IS NOT NULL
       AND (p.id_persona IS NULL OR p.tipo_persona <> 'C');

    -- R7: el vehiculo debe existir.
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT f.id_trayecto_tmp,
           CONCAT('El vehiculo (id_vehiculo ', f.vehiculo_n, ') no existe')
      FROM tmp_val_filas f
      LEFT JOIN vehiculos v ON v.id_vehiculo = f.vehiculo_n
     WHERE f.vehiculo_n IS NOT NULL
       AND v.id_vehiculo IS NULL;

    -- R8: el conductor debe estar asociado a ESTE vehiculo y en estado PO (Puede Operar).
    -- Los JOIN (no LEFT) a persona y vehiculos hacen que solo se evalue cuando AMBOS existen y la persona es
    -- 'C': asi no se repite el error de R6/R7 con otro mensaje. Cualquier estado distinto de PO (RO, EA) falla,
    -- igual que en crearRuta.
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT f.id_trayecto_tmp,
           CASE WHEN vp.id_persona IS NULL
                     THEN CONCAT('El conductor ', f.persona_n, ' no esta asociado al vehiculo ', f.vehiculo_n)
                ELSE CONCAT('El conductor ', f.persona_n, ' no esta en estado PO para el vehiculo ',
                            f.vehiculo_n, ' (estado actual: ', vp.estado_conductor, ')')
           END
      FROM tmp_val_filas f
      JOIN persona   p ON p.id_persona  = f.persona_n AND p.tipo_persona = 'C'
      JOIN vehiculos v ON v.id_vehiculo = f.vehiculo_n
      LEFT JOIN vehiculo_persona vp ON vp.id_vehiculo = f.vehiculo_n AND vp.id_persona = f.persona_n
     WHERE vp.id_persona IS NULL OR vp.estado_conductor <> 'PO';

    -- R9: "vehiculo habilitado" = tiene AL MENOS un documento y TODOS estan en 'Habilitado'.
    -- La subconsulta cuenta los documentos de cada vehiculo y cuantos NO estan habilitados
    -- (la comparacion da 1 o 0 y SUM los suma). Si el vehiculo no aparece en ella, no tiene documentos.
    -- Es la misma definicion que usa la consulta publica de rutas restringidas.
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT f.id_trayecto_tmp,
           CASE WHEN d.total IS NULL
                     THEN CONCAT('El vehiculo ', f.vehiculo_n, ' no tiene documentos')
                ELSE CONCAT('El vehiculo ', f.vehiculo_n, ' no tiene todos sus documentos Habilitados (',
                            d.pendientes, ' de ', d.total, ' sin habilitar)')
           END
      FROM tmp_val_filas f
      JOIN vehiculos v ON v.id_vehiculo = f.vehiculo_n
      LEFT JOIN (SELECT id_vehiculo,
                        COUNT(*) AS total,
                        CAST(SUM(estado_documento <> 'Habilitado') AS UNSIGNED) AS pendientes
                   FROM vehiculo_documento
                  GROUP BY id_vehiculo) d
             ON d.id_vehiculo = f.vehiculo_n
     WHERE d.total IS NULL OR d.pendientes > 0;

    -- R10: el codigo de ruta no puede existir ya en TRAYECTO (el codigo agrupa las paradas de una ruta).
    -- La comparacion no distingue mayusculas ('ruta-a' = 'RUTA-A'), igual que la llave unica de trayecto.
    -- sp_procesado vuelve a revisarlo por si otro cargue lo proceso entre la validacion y el traslado.
    INSERT INTO tmp_val_errores (id_trayecto_tmp, mensaje)
    SELECT f.id_trayecto_tmp,
           LEFT(CONCAT('Ya existe una ruta con el codigo ', f.ruta, ' en trayecto'), 200)
      FROM tmp_val_filas f
     WHERE f.ruta IS NOT NULL
       AND EXISTS (SELECT 1 FROM trayecto t WHERE t.codigo_ruta = f.ruta);

    DROP TEMPORARY TABLE IF EXISTS tmp_val_filas;
    -- FIN DE LAS REGLAS: sigue el CIERRE

    -- ===================== CIERRE: decidir el estado de cada fila =====================

    -- CIERRE 1: rutas que tienen al menos una fila con error propio (para "ruta completa o nada")
    INSERT INTO tmp_val_rutas (codigo_ruta)
    SELECT DISTINCT TRIM(t.codigo_ruta)
      FROM tmp_val_errores e
      JOIN trayecto_tmp t ON t.id_trayecto_tmp = e.id_trayecto_tmp
     WHERE t.codigo_ruta IS NOT NULL AND TRIM(t.codigo_ruta) <> '';

    -- CIERRE 2: las filas con error propio pasan a ERROR con TODOS sus mensajes, en el orden de las reglas.
    -- CONCAT_WS ignora los NULL: si observacion estaba vacia queda solo el mensaje; si no, se agrega con '; '.
    UPDATE trayecto_tmp t
      JOIN (SELECT id_trayecto_tmp,
                   GROUP_CONCAT(mensaje ORDER BY seq SEPARATOR '; ') AS mensajes
              FROM tmp_val_errores
             GROUP BY id_trayecto_tmp) e
        ON e.id_trayecto_tmp = t.id_trayecto_tmp
       SET t.estado = 'ERROR',
           t.observacion = CONCAT_WS('; ', t.observacion, e.mensajes);

    -- CIERRE 3: RUTA COMPLETA O NADA. Las filas que siguen CARGADO pero pertenecen a una ruta con
    -- errores tambien se rechazan, con una explicacion.
    UPDATE trayecto_tmp t
      JOIN tmp_val_rutas r ON r.codigo_ruta = TRIM(t.codigo_ruta)
       SET t.estado = 'ERROR',
           t.observacion = CONCAT_WS('; ', t.observacion,
                           'Otra parada de esta ruta tiene errores; la ruta se rechaza completa')
     WHERE t.id_cargue = p_id_cargue AND t.estado = 'CARGADO';

    -- CIERRE 4: lo que sigue CARGADO paso todas las reglas
    UPDATE trayecto_tmp
       SET estado = 'VALIDADO'
     WHERE id_cargue = p_id_cargue AND estado = 'CARGADO';

    -- LIMPIEZA: las conexiones se reutilizan, asi que no se dejan tablas temporales vivas
    DROP TEMPORARY TABLE IF EXISTS tmp_val_errores;
    DROP TEMPORARY TABLE IF EXISTS tmp_val_rutas;

    -- IMPORTANTE: sin SELECT final, sin COMMIT, sin START TRANSACTION (igual que los otros dos).
END;


-- =====================================================================================================
-- sp_procesado: PASO 3 del cargue. Traslada a TRAYECTO las rutas completas y validadas de un cargue.
-- Confia en sp_validacion: aqui NO se vuelven a revisar las reglas de negocio, solo se convierte y se mueve.
-- Es seguro ejecutarlo dos veces: la segunda vez ya no hay filas VALIDADO y no hace nada.
-- =====================================================================================================
DROP PROCEDURE IF EXISTS sp_procesado;

CREATE PROCEDURE sp_procesado(
    IN p_id_cargue BIGINT          -- el cargue a procesar (yyyyMMddHHmm)
)
BEGIN
    -- PASO 1 - RE-CHEQUEO DE CODIGOS REPETIDOS.
    -- sp_validacion ya reviso que el codigo de ruta no existiera en trayecto, pero entre la validacion
    -- y este momento otro cargue pudo haber procesado ese mismo codigo. Si es asi, estas filas pasan a ERROR.
    -- CONCAT_WS ignora los NULL: si observacion estaba vacia queda solo el mensaje; si no, se agrega con '; '.
    UPDATE trayecto_tmp
       SET estado = 'ERROR',
           observacion = CONCAT_WS('; ', observacion,
                         'La ruta ya existe en trayecto (fue procesada en otro cargue)')
     WHERE id_cargue = p_id_cargue
       AND estado = 'VALIDADO'
       AND EXISTS (SELECT 1 FROM trayecto t
                    WHERE t.codigo_ruta = TRIM(trayecto_tmp.codigo_ruta));

    -- PASO 2 - TRASLADO A TRAYECTO (una sola sentencia para TODAS las filas, sin ciclos).
    -- Aqui se convierte el texto a los tipos reales. TRIM quita espacios sobrantes: sp_cargue no podia
    -- tocar nada, pero al entrar a la tabla definitiva si limpiamos. sp_validacion mira el valor
    -- ya con TRIM, para que lo validado sea exactamente lo que se inserta.
    -- NULLIF(TRIM(x), '') convierte una cadena vacia en NULL; CAST(NULL ...) sigue siendo NULL.
    INSERT INTO trayecto
        (id_persona, id_vehiculo, codigo_ruta, ubicacion, orden_parada, latitud, longitud, login_usuario)
    SELECT CAST(TRIM(v.id_persona)  AS UNSIGNED),
           CAST(TRIM(v.id_vehiculo) AS UNSIGNED),
           TRIM(v.codigo_ruta),
           TRIM(v.ubicacion),
           CAST(TRIM(v.orden_parada) AS UNSIGNED),
           CAST(NULLIF(TRIM(v.latitud),  '') AS DECIMAL(10,8)),
           CAST(NULLIF(TRIM(v.longitud), '') AS DECIMAL(11,8)),
           TRIM(v.login_usuario)
      FROM trayecto_tmp v
     WHERE v.id_cargue = p_id_cargue
       AND v.estado = 'VALIDADO'
       -- RUTA COMPLETA O NADA: solo pasa la ruta si NINGUNA de sus paradas (en este cargue) esta en otro estado
       AND NOT EXISTS (SELECT 1 FROM trayecto_tmp o
                        WHERE o.id_cargue = v.id_cargue
                          AND TRIM(o.codigo_ruta) = TRIM(v.codigo_ruta)
                          AND o.estado <> 'VALIDADO')
     ORDER BY v.id_trayecto_tmp;

    -- PASO 3 - MARCAR COMO PROCESADO lo que acaba de entrar a trayecto.
    -- El criterio es "su codigo ya existe en trayecto" (otra tabla), NO "no tiene hermanas en otro estado".
    -- Si usaramos el criterio del paso 2, este UPDATE miraria filas que el mismo UPDATE esta cambiando.
    -- Los codigos que ya existian antes fueron apartados en el paso 1, asi que solo quedan las recien insertadas.
    UPDATE trayecto_tmp
       SET estado = 'PROCESADO'
     WHERE id_cargue = p_id_cargue
       AND estado = 'VALIDADO'
       AND EXISTS (SELECT 1 FROM trayecto t
                    WHERE t.codigo_ruta = TRIM(trayecto_tmp.codigo_ruta));

    -- PASO 4 - CIERRE: lo que sigue VALIDADO no se pudo trasladar (su ruta tiene paradas que no estan validadas).
    -- Se marca ERROR con su explicacion, asi un cargue procesado solo termina en PROCESADO o en ERROR.
    UPDATE trayecto_tmp
       SET estado = 'ERROR',
           observacion = CONCAT_WS('; ', observacion,
                         'La ruta no se pudo procesar porque tiene paradas sin validar')
     WHERE id_cargue = p_id_cargue
       AND estado = 'VALIDADO';

    -- IMPORTANTE: sin SELECT final, sin COMMIT, sin START TRANSACTION (igual que sp_cargue).
END;
