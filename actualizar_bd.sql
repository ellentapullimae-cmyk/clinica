-- =============================================================
-- SISTEMA DE GESTION PARA UNA CLINICA
-- Actualizacion para el modulo de usuarios y login.
-- Este script es INCREMENTAL: NO elimina tablas ni datos existentes.
-- Puede ejecutarse cuantas veces sea necesario (es idempotente).
--
-- Usuarios creados por defecto:
--   admin          / admin123    (Administrador)
--   recepcionista  / recep123    (Recepcionista)
--   medico         / medico123   (Medico, vinculado al Dr. Carlos Vargas Luna)
--
-- Las claves se almacenan como hash SHA-256 con sal (nunca en texto plano).
-- =============================================================

USE clinica;

-- -------------------------------------------------------------
-- USUARIO (login y control de acceso)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuario (
  id_usuario      INT AUTO_INCREMENT PRIMARY KEY,
  nombre_usuario  VARCHAR(30)  NOT NULL UNIQUE,
  nombre_completo VARCHAR(120) NOT NULL,
  clave_hash      CHAR(64)     NOT NULL,
  sal             VARCHAR(100) NOT NULL,
  rol             VARCHAR(20)  NOT NULL DEFAULT 'RECEPCIONISTA',
  id_medico       INT NULL,
  estado          VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',
  fecha_registro  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_usuario_medico    FOREIGN KEY (id_medico) REFERENCES medico(id_medico),
  CONSTRAINT chk_usuario_rol      CHECK (rol IN ('ADMINISTRADOR','RECEPCIONISTA','MEDICO')),
  CONSTRAINT chk_usuario_estado   CHECK (estado IN ('ACTIVO','INACTIVO'))
);

-- Usuario administrador por defecto.
INSERT INTO usuario (nombre_usuario, nombre_completo, clave_hash, sal, rol, id_medico, estado)
SELECT 'admin', 'Administrador del Sistema',
       '1aa45bb1b8e9cae4ba80b4c7a22156b03c65ff2e5c3aeafc68f5e1c89811ce96',
       'SAL_ADMIN_2026', 'ADMINISTRADOR', NULL, 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE nombre_usuario = 'admin');

-- Usuario recepcionista por defecto.
INSERT INTO usuario (nombre_usuario, nombre_completo, clave_hash, sal, rol, id_medico, estado)
SELECT 'recepcionista', 'Recepcionista de la Clinica',
       'f6aa796b8f183eeb898c4a7fe1fde13669e8cb33c5d385ff4bfde0cb8446aa01',
       'SAL_RECEP_2026', 'RECEPCIONISTA', NULL, 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE nombre_usuario = 'recepcionista');

-- Usuario medico por defecto (vinculado al medico registrado id 1).
INSERT INTO usuario (nombre_usuario, nombre_completo, clave_hash, sal, rol, id_medico, estado)
SELECT 'medico', 'Dr. Carlos Vargas Luna',
       '5f5b955c44b2d65b0130996a55202f328705f17c4da15ac2a54c1b4f8e644ba1',
       'SAL_MEDIC_2026', 'MEDICO', 1, 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE nombre_usuario = 'medico');

-- -------------------------------------------------------------
-- INDICES para acelerar las consultas por rango de fechas
-- y los conteos por estado/rol (idempotentes en MariaDB/MySQL 8).
-- -------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_cita_fecha        ON cita (fecha);
CREATE INDEX IF NOT EXISTS idx_cita_medico       ON cita (id_medico);
CREATE INDEX IF NOT EXISTS idx_cita_paciente     ON cita (id_paciente);
CREATE INDEX IF NOT EXISTS idx_atencion_fecha    ON atencion (fecha_atencion);
CREATE INDEX IF NOT EXISTS idx_atencion_paciente ON atencion (id_paciente);
CREATE INDEX IF NOT EXISTS idx_pago_fecha        ON pago (fecha);
CREATE INDEX IF NOT EXISTS idx_gasto_fecha_estado ON gasto (fecha, estado);
CREATE INDEX IF NOT EXISTS idx_usuario_rol       ON usuario (rol);