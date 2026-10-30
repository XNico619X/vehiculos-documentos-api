-- Entrega 4: tabla temporal donde entra el Excel de cargue de trayectos SIN modificar nada.
CREATE TABLE trayecto_tmp (
  id_trayecto_tmp BIGINT AUTO_INCREMENT PRIMARY KEY,
  -- Campos replica de TRAYECTO, todos como TEXTO y mas anchos: el Excel entra sin alteracion
  id_persona    VARCHAR(50)  NULL,
  id_vehiculo   VARCHAR(50)  NULL,
  codigo_ruta   VARCHAR(100) NULL,
  ubicacion     VARCHAR(500) NULL,
  orden_parada  VARCHAR(50)  NULL,
  latitud       VARCHAR(50)  NULL,
  longitud      VARCHAR(50)  NULL,
  login_usuario VARCHAR(100) NULL,
  -- Campos propios del cargue
  estado        VARCHAR(10)  NOT NULL DEFAULT 'CARGADO',
  observacion   TEXT         NULL,
  id_cargue     BIGINT       NOT NULL,
  CONSTRAINT chk_trayecto_tmp_estado CHECK (estado IN ('CARGADO','VALIDADO','PROCESADO','ERROR')),
  INDEX idx_trayecto_tmp_cargue (id_cargue)
);