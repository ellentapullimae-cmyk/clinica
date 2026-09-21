-- =============================================================
-- SISTEMA DE GESTION PARA UNA CLINICA
-- Base de datos MySQL / MariaDB
-- Modulos: pacientes, medicos, citas, atenciones, pagos, gastos,
--          reportes y presupuesto.
-- IESTP "PAIJAN" - Programacion Orientada a Objetos - 2026
-- NOTA: al ejecutar este script la base de datos se crea de cero
--       (las tablas existentes se eliminan y se vuelven a crear).
-- =============================================================

CREATE DATABASE IF NOT EXISTS clinica
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE clinica;

-- Se eliminan tablas existentes para dejar un estado limpio.
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS pago;
DROP TABLE IF EXISTS atencion;
DROP TABLE IF EXISTS cita;
DROP TABLE IF EXISTS gasto;
DROP TABLE IF EXISTS medico;
DROP TABLE IF EXISTS paciente;
DROP TABLE IF EXISTS configuracion;

-- -------------------------------------------------------------
-- PACIENTE (RF-01)
-- Regla: el DNI es unico y debe tener 8 digitos.
-- -------------------------------------------------------------
CREATE TABLE paciente (
  id_paciente    INT AUTO_INCREMENT PRIMARY KEY,
  nombre         VARCHAR(120) NOT NULL,
  dni            CHAR(8) NOT NULL UNIQUE,
  telefono       VARCHAR(15),
  correo         VARCHAR(100),
  direccion      VARCHAR(200),
  fecha_registro DATE NOT NULL,
  estado         VARCHAR(10) NOT NULL DEFAULT 'ACTIVO',
  CONSTRAINT chk_paciente_dni CHECK (dni REGEXP '^[0-9]{8}$'),
  CONSTRAINT chk_paciente_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

-- -------------------------------------------------------------
-- MEDICO (RF-02)
-- -------------------------------------------------------------
CREATE TABLE medico (
  id_medico    INT AUTO_INCREMENT PRIMARY KEY,
  nombre       VARCHAR(120) NOT NULL,
  dni          CHAR(8) NOT NULL UNIQUE,
  especialidad VARCHAR(80) NOT NULL,
  telefono     VARCHAR(15),
  fecha_registro DATE NOT NULL,
  estado       VARCHAR(10) NOT NULL DEFAULT 'ACTIVO',
  CONSTRAINT chk_medico_dni CHECK (dni REGEXP '^[0-9]{8}$'),
  CONSTRAINT chk_medico_estado CHECK (estado IN ('ACTIVO','INACTIVO'))
);

-- -------------------------------------------------------------
-- CITA (RF-03)
-- Regla: no se asignan dos citas al mismo medico en el mismo
--        horario (se controla ademas en la capa de servicio).
-- -------------------------------------------------------------
CREATE TABLE cita (
  id_cita     INT AUTO_INCREMENT PRIMARY KEY,
  id_paciente INT NOT NULL,
  id_medico   INT NOT NULL,
  fecha       DATE NOT NULL,
  hora        TIME NOT NULL,
  estado      VARCHAR(12) NOT NULL DEFAULT 'PROGRAMADA',
  observacion VARCHAR(200),
  CONSTRAINT fk_cita_paciente FOREIGN KEY (id_paciente) REFERENCES paciente(id_paciente),
  CONSTRAINT fk_cita_medico   FOREIGN KEY (id_medico)   REFERENCES medico(id_medico),
  CONSTRAINT chk_cita_estado  CHECK (estado IN ('PROGRAMADA','CONFIRMADA','CANCELADA','ATENDIDA'))
);

-- -------------------------------------------------------------
-- ATENCION (RF-04, RF-09)
-- Regla: una atencion debe relacionarse con una cita
--        (id_cita es UNICO: una cita genera una sola atencion).
-- -------------------------------------------------------------
CREATE TABLE atencion (
  id_atencion    INT AUTO_INCREMENT PRIMARY KEY,
  id_cita        INT NOT NULL UNIQUE,
  id_paciente    INT NOT NULL,
  diagnostico    VARCHAR(300) NOT NULL,
  observaciones  VARCHAR(500),
  fecha_atencion DATE NOT NULL,
  CONSTRAINT fk_atencion_cita     FOREIGN KEY (id_cita)     REFERENCES cita(id_cita),
  CONSTRAINT fk_atencion_paciente FOREIGN KEY (id_paciente) REFERENCES paciente(id_paciente),
  INDEX idx_atencion_fecha  (fecha_atencion),
  INDEX idx_atencion_paciente(id_paciente)
);

-- -------------------------------------------------------------
-- PAGO (RF-05)
-- Regla: el monto debe ser mayor que cero. Puede vincularse a
--        una atencion (relacion Pago -> Atencion) o ser directo.
-- -------------------------------------------------------------
CREATE TABLE pago (
  id_pago     INT AUTO_INCREMENT PRIMARY KEY,
  id_atencion INT NULL,
  monto       DECIMAL(10,2) NOT NULL,
  fecha       DATE NOT NULL,
  metodo      VARCHAR(20) NOT NULL,
  CONSTRAINT fk_pago_atencion FOREIGN KEY (id_atencion) REFERENCES atencion(id_atencion),
  CONSTRAINT chk_pago_monto  CHECK (monto > 0),
  CONSTRAINT chk_pago_metodo CHECK (metodo IN ('EFECTIVO','TARJETA','TRANSFERENCIA','OTRO')),
  INDEX idx_pago_fecha (fecha)
);

-- -------------------------------------------------------------
-- GASTO (RF-06)
-- Reglas: descripcion, categoria, fecha y monto obligatorios;
--         monto > 0; los gastos ANULADOS no entran en los totales.
-- -------------------------------------------------------------
CREATE TABLE gasto (
  id_gasto    INT AUTO_INCREMENT PRIMARY KEY,
  descripcion VARCHAR(200) NOT NULL,
  monto       DECIMAL(10,2) NOT NULL,
  fecha       DATE NOT NULL,
  categoria   VARCHAR(20) NOT NULL,
  estado      VARCHAR(20) NOT NULL DEFAULT 'CONTABILIZADO',
  CONSTRAINT chk_gasto_monto  CHECK (monto > 0),
  CONSTRAINT chk_gasto_categoria CHECK (categoria IN ('INSUMOS','SERVICIOS','MANTENIMIENTO')),
  CONSTRAINT chk_gasto_estado CHECK (estado IN ('CONTABILIZADO','ANULADO')),
  INDEX idx_gasto_fecha_estado (fecha, estado)
);

-- -------------------------------------------------------------
-- USUARIO (login y control de acceso)
-- Reglas: nombre de usuario unico; la clave se guarda como hash
--         SHA-256 con sal (nunca en texto plano).
-- -------------------------------------------------------------
CREATE TABLE usuario (
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
  CONSTRAINT chk_usuario_estado   CHECK (estado IN ('ACTIVO','INACTIVO')),
  INDEX idx_usuario_rol (rol)
);

-- -------------------------------------------------------------
-- CONFIGURACION (presupuesto del periodo)
-- Saldo del presupuesto = Presupuesto - Gastos.
-- -------------------------------------------------------------
CREATE TABLE configuracion (
  clave VARCHAR(50) PRIMARY KEY,
  valor DECIMAL(10,2) NOT NULL
);

-- =============================================================
-- DATOS DE PRUEBA
-- =============================================================

INSERT INTO configuracion (clave, valor) VALUES ('presupuesto_periodo', 2000.00);

INSERT INTO paciente (id_paciente, nombre, dni, telefono, correo, direccion, fecha_registro, estado) VALUES
  (1, 'Ana Maria Gutierrez Flores', '73451236', '984123456', 'ana.gutierrez@example.com', 'Av. Los Pinos 123, Paijan',     '2026-09-01', 'ACTIVO'),
  (2, 'Pedro Luis Ramirez Torres',  '71556678', '976543210', 'pedro.ramirez@example.com', 'Jr. Las Rosas 456, Paijan',    '2026-09-02', 'ACTIVO'),
  (3, 'Rosa Elena Quispe Vargas',   '72540129', '912345678', 'rosa.quispe@example.com',   'Calle San Martin 789, Paijan', '2026-09-03', 'ACTIVO');

INSERT INTO medico (id_medico, nombre, dni, especialidad, telefono, fecha_registro, estado) VALUES
  (1, 'Dr. Carlos Vargas Luna',    '40112233', 'Medicina General', '994112233', '2026-08-01', 'ACTIVO'),
  (2, 'Dra. Lucia Torres Diaz',    '41112233', 'Pediatria',        '994223344', '2026-08-02', 'ACTIVO'),
  (3, 'Dr. Jorge Salas Mendoza',   '42112233', 'Odontologia',      '994334455', '2026-08-03', 'ACTIVO');

INSERT INTO cita (id_cita, id_paciente, id_medico, fecha, hora, estado, observacion) VALUES
  (1, 1, 1, '2026-09-18', '09:00:00', 'ATENDIDA',   'Consulta general programada'),
  (2, 2, 2, '2026-09-18', '10:00:00', 'ATENDIDA',   'Control pediatrico'),
  (3, 1, 1, '2026-09-24', '11:00:00', 'CONFIRMADA', 'Segunda consulta'),
  (4, 3, 3, '2026-09-18', '17:00:00', 'ATENDIDA',   'Limpieza dental'),
  (5, 2, 2, '2026-09-28', '08:30:00', 'PROGRAMADA', 'Vacunas'),
  (6, 3, 1, '2026-09-26', '09:00:00', 'CANCELADA',  'Cancelada por el paciente'),
  (7, 1, 3, '2026-09-25', '15:00:00', 'PROGRAMADA', 'Evaluacion dental');

INSERT INTO atencion (id_atencion, id_cita, id_paciente, diagnostico, observaciones, fecha_atencion) VALUES
  (1, 1, 1, 'Chequeo general sin hallazgos relevantes',
      'Se recomienda revision en 6 meses', '2026-09-18'),
  (2, 2, 2, 'Control de crecimiento dentro de lo normal',
      'Peso y talla adecuados para la edad', '2026-09-18'),
  (3, 4, 3, 'Limpieza dental y revision completa',
      'Salud bucal satisfactoria', '2026-09-18');

INSERT INTO pago (id_pago, id_atencion, monto, fecha, metodo) VALUES
  (1, 1, 120.00, '2026-09-18', 'EFECTIVO'),
  (2, 2, 100.00, '2026-09-18', 'TARJETA'),
  (3, 3, 250.00, '2026-09-19', 'TRANSFERENCIA');

-- Gastos G-001 a G-005 del documento.
-- Total: S/ 1,180.00  |  Presupuesto: S/ 2,000.00  |  Saldo: S/ 820.00
INSERT INTO gasto (descripcion, monto, fecha, categoria, estado) VALUES
  ('Compra de medicamentos',         350.00, '2026-08-20', 'INSUMOS',       'CONTABILIZADO'),
  ('Pago de electricidad',           280.00, '2026-08-21', 'SERVICIOS',     'CONTABILIZADO'),
  ('Guantes y materiales medicos',   180.00, '2026-08-22', 'INSUMOS',       'CONTABILIZADO'),
  ('Mantenimiento de equipo medico', 250.00, '2026-08-23', 'MANTENIMIENTO', 'CONTABILIZADO'),
  ('Pago mensual de internet',       120.00, '2026-08-24', 'SERVICIOS',     'CONTABILIZADO');

-- Usuarios por defecto (login y control de acceso).
-- Claves por defecto:
--   admin / admin123 (Administrador)
--   recepcionista / recep123 (Recepcionista)
--   medico / medico123 (Medico, vinculado al Dr. Carlos Vargas Luna)
INSERT INTO usuario (nombre_usuario, nombre_completo, clave_hash, sal, rol, id_medico, estado) VALUES
  ('admin',  'Administrador del Sistema',
      '1aa45bb1b8e9cae4ba80b4c7a22156b03c65ff2e5c3aeafc68f5e1c89811ce96',
      'SAL_ADMIN_2026', 'ADMINISTRADOR', NULL, 'ACTIVO'),
  ('recepcionista', 'Recepcionista de la Clinica',
      'f6aa796b8f183eeb898c4a7fe1fde13669e8cb33c5d385ff4bfde0cb8446aa01',
      'SAL_RECEP_2026', 'RECEPCIONISTA', NULL, 'ACTIVO'),
  ('medico', 'Dr. Carlos Vargas Luna',
      '5f5b955c44b2d65b0130996a55202f328705f17c4da15ac2a54c1b4f8e644ba1',
      'SAL_MEDIC_2026', 'MEDICO', 1, 'ACTIVO');