-- ============================================================
-- SISTEMA CLÍNICAS PSICOLOGÍA DEL PERÚ
-- Base de datos: SQL Server
-- ============================================================

USE master;
GO

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'ClinicaPsicologia')
BEGIN
    CREATE DATABASE ClinicaPsicologia;
END
GO

USE ClinicaPsicologia;
GO

-- ============================================================
-- TABLA: Roles del sistema
-- ============================================================
IF OBJECT_ID('dbo.Rol', 'U') IS NULL
CREATE TABLE Rol (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    nombre        VARCHAR(50)  NOT NULL UNIQUE,  -- recepcion | caja | psicologo | admin
    descripcion   VARCHAR(200)
);

-- ============================================================
-- TABLA: Usuarios del sistema
-- ============================================================
IF OBJECT_ID('dbo.Usuario', 'U') IS NULL
CREATE TABLE Usuario (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    rol_id          INT          NOT NULL REFERENCES Rol(id),
    activo          BIT          NOT NULL DEFAULT 1,
    creado_en       DATETIME2    NOT NULL DEFAULT GETDATE()
);

-- ============================================================
-- TABLA: Especialidades psicológicas
-- ============================================================
IF OBJECT_ID('dbo.Especialidad', 'U') IS NULL
CREATE TABLE Especialidad (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(300),
    tarifa      DECIMAL(10,2) NOT NULL DEFAULT 80.00,
    activo      BIT NOT NULL DEFAULT 1
);

-- ============================================================
-- TABLA: Psicólogos
-- ============================================================
IF OBJECT_ID('dbo.Psicologo', 'U') IS NULL
CREATE TABLE Psicologo (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    usuario_id      INT          NULL REFERENCES Usuario(id),
    nombres         VARCHAR(100) NOT NULL,
    apellidos       VARCHAR(100) NOT NULL,
    colegiatura     VARCHAR(50)  NOT NULL UNIQUE,
    telefono        VARCHAR(20),
    email           VARCHAR(150),
    especialidad_id INT          NOT NULL REFERENCES Especialidad(id),
    activo          BIT          NOT NULL DEFAULT 1
);

-- ============================================================
-- TABLA: Horarios de psicólogos
-- Lunes(1) a Sábado(6), 08:00 - 19:00
-- ============================================================
IF OBJECT_ID('dbo.HorarioPsicologo', 'U') IS NULL
CREATE TABLE HorarioPsicologo (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    psicologo_id    INT  NOT NULL REFERENCES Psicologo(id),
    dia_semana      TINYINT NOT NULL,  -- 1=Lun ... 6=Sab
    hora_inicio     TIME NOT NULL,
    hora_fin        TIME NOT NULL,
    activo          BIT  NOT NULL DEFAULT 1,
    CONSTRAINT UQ_Horario UNIQUE (psicologo_id, dia_semana, hora_inicio)
);

-- ============================================================
-- TABLA: Pacientes / Historia Clínica
-- ============================================================
IF OBJECT_ID('dbo.Paciente', 'U') IS NULL
CREATE TABLE Paciente (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    numero_historia     VARCHAR(20)  NOT NULL UNIQUE,  -- HC-0001
    dni                 VARCHAR(8)   NOT NULL UNIQUE,
    nombres             VARCHAR(100) NOT NULL,
    apellidos           VARCHAR(100) NOT NULL,
    fecha_nacimiento    DATE         NOT NULL,
    sexo                CHAR(1)      NOT NULL CHECK (sexo IN ('M','F')),
    telefono            VARCHAR(20),
    email               VARCHAR(150),
    direccion           VARCHAR(250),
    fecha_apertura      DATETIME2    NOT NULL DEFAULT GETDATE(),
    activo              BIT          NOT NULL DEFAULT 1
);

-- ============================================================
-- TABLA: Tickets de atención (sistema de cola)
-- ============================================================
IF OBJECT_ID('dbo.Ticket', 'U') IS NULL
CREATE TABLE Ticket (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    numero          VARCHAR(10)  NOT NULL,   -- A-042
    fecha_emision   DATETIME2    NOT NULL DEFAULT GETDATE(),
    estado          VARCHAR(20)  NOT NULL DEFAULT 'EN_ESPERA'
                    CHECK (estado IN ('EN_ESPERA','EN_ATENCION','FINALIZADO')),
    paciente_id     INT          NULL REFERENCES Paciente(id)
);

-- ============================================================
-- TABLA: Citas
-- ============================================================
IF OBJECT_ID('dbo.Cita', 'U') IS NULL
CREATE TABLE Cita (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    paciente_id     INT          NOT NULL REFERENCES Paciente(id),
    psicologo_id    INT          NOT NULL REFERENCES Psicologo(id),
    especialidad_id INT          NOT NULL REFERENCES Especialidad(id),
    ticket_id       INT          NULL REFERENCES Ticket(id),
    fecha_cita      DATE         NOT NULL,
    hora_cita       TIME         NOT NULL,
    estado          VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE_PAGO'
                    CHECK (estado IN ('PENDIENTE_PAGO','PAGADA','EN_CONSULTA','ATENDIDA','CANCELADA')),
    creado_por      INT          NULL REFERENCES Usuario(id),
    creado_en       DATETIME2    NOT NULL DEFAULT GETDATE()
);

-- ============================================================
-- TABLA: Deudas / Cuentas por cobrar
-- ============================================================
IF OBJECT_ID('dbo.Deuda', 'U') IS NULL
CREATE TABLE Deuda (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    paciente_id     INT             NOT NULL REFERENCES Paciente(id),
    cita_id         INT             NULL REFERENCES Cita(id),
    concepto        VARCHAR(100)    NOT NULL,   -- 'Gastos de cita' | 'Sesion de Terapia'
    monto           DECIMAL(10,2)   NOT NULL,
    estado          VARCHAR(20)     NOT NULL DEFAULT 'PENDIENTE'
                    CHECK (estado IN ('PENDIENTE','PAGADO')),
    creado_en       DATETIME2       NOT NULL DEFAULT GETDATE()
);

-- ============================================================
-- TABLA: Comprobantes de pago
-- ============================================================
IF OBJECT_ID('dbo.ComprobantePago', 'U') IS NULL
CREATE TABLE ComprobantePago (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    numero_comprobante  VARCHAR(20)     NOT NULL UNIQUE,  -- B-00001
    tipo                VARCHAR(20)     NOT NULL DEFAULT 'BOLETA'
                        CHECK (tipo IN ('BOLETA','FACTURA')),
    deuda_id            INT             NOT NULL REFERENCES Deuda(id),
    medio_pago          VARCHAR(30)     NOT NULL
                        CHECK (medio_pago IN ('EFECTIVO','TARJETA','YAPE_PLIN')),
    monto_pagado        DECIMAL(10,2)   NOT NULL,
    fecha_pago          DATETIME2       NOT NULL DEFAULT GETDATE(),
    cajero_id           INT             NULL REFERENCES Usuario(id)
);

-- ============================================================
-- TABLA: Proceso terapéutico (fases globales del paciente)
-- Fases según UNIR: Evaluación, Explicación de Hipótesis,
--                   Tratamiento, Seguimiento
-- ============================================================
IF OBJECT_ID('dbo.ProcesoTerapeutico', 'U') IS NULL
CREATE TABLE ProcesoTerapeutico (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    paciente_id     INT          NOT NULL REFERENCES Paciente(id),
    psicologo_id    INT          NOT NULL REFERENCES Psicologo(id),
    fase_actual     TINYINT      NOT NULL DEFAULT 1
                    CHECK (fase_actual BETWEEN 1 AND 4),
    -- 1=Evaluacion | 2=Explicacion Hipotesis | 3=Tratamiento | 4=Seguimiento
    fecha_inicio    DATE         NOT NULL DEFAULT CAST(GETDATE() AS DATE),
    fecha_fin       DATE         NULL,
    observaciones   NVARCHAR(MAX) NULL,
    activo          BIT          NOT NULL DEFAULT 1,
    CONSTRAINT UQ_ProcTer UNIQUE (paciente_id, psicologo_id, activo)
);

-- ============================================================
-- TABLA: Sesiones (resultado de cada cita psicológica)
-- ============================================================
IF OBJECT_ID('dbo.Sesion', 'U') IS NULL
CREATE TABLE Sesion (
    id                      INT IDENTITY(1,1) PRIMARY KEY,
    cita_id                 INT           NOT NULL REFERENCES Cita(id),
    proceso_terapeutico_id  INT           NOT NULL REFERENCES ProcesoTerapeutico(id),
    fase_sesion             TINYINT       NOT NULL CHECK (fase_sesion BETWEEN 1 AND 4),
    evolucion               NVARCHAR(MAX) NOT NULL,  -- notas clínicas
    indicaciones_paciente   NVARCHAR(MAX) NULL,      -- tareas / tratamiento
    fecha_registro          DATETIME2     NOT NULL DEFAULT GETDATE(),
    registrado_por          INT           NULL REFERENCES Usuario(id)
);

-- ============================================================
-- DATOS INICIALES
-- ============================================================

-- Roles
IF NOT EXISTS (SELECT 1 FROM Rol WHERE nombre = 'ADMIN')
INSERT INTO Rol (nombre, descripcion) VALUES
    ('ADMIN',      'Administrador del sistema'),
    ('RECEPCION',  'Personal de recepción'),
    ('CAJA',       'Cajero de consultorios'),
    ('PSICOLOGO',  'Psicólogo tratante');

-- Especialidades
IF NOT EXISTS (SELECT 1 FROM Especialidad WHERE nombre = 'Psicología Clínica')
INSERT INTO Especialidad (nombre, descripcion, tarifa) VALUES
    ('Psicología Clínica',       'Evaluación y tratamiento de trastornos psicológicos',             80.00),
    ('Psicología Infantil',      'Atención especializada en niños y adolescentes',                  80.00),
    ('Psicología Organizacional','Intervención en entornos laborales y organizacionales',           90.00),
    ('Neuropsicología',          'Evaluación de funciones cognitivas y conductuales',              100.00),
    ('Terapia de Pareja',        'Intervención en dinámicas relacionales y familiares',             90.00);

-- Usuarios iniciales (password: "123" -> BCrypt hash)
-- Hash generado para "123": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh3y
IF NOT EXISTS (SELECT 1 FROM Usuario WHERE username = 'admin')
BEGIN
    DECLARE @hashPass VARCHAR(255) = '$2a$10$2OrRLP6u9I0Txwl8gNJmPeQOI4BSAPZt865HXfsSwm1tuVQmshkWm';
    DECLARE @rolAdmin INT    = (SELECT id FROM Rol WHERE nombre = 'ADMIN');
    DECLARE @rolRecep INT    = (SELECT id FROM Rol WHERE nombre = 'RECEPCION');
    DECLARE @rolCaja  INT    = (SELECT id FROM Rol WHERE nombre = 'CAJA');
    DECLARE @rolPsic  INT    = (SELECT id FROM Rol WHERE nombre = 'PSICOLOGO');

    INSERT INTO Usuario (username, password_hash, nombre_completo, rol_id) VALUES
        ('admin',      @hashPass, 'Administrador del Sistema',   @rolAdmin),
        ('recepcion',  @hashPass, 'María Recepcionista López',   @rolRecep),
        ('caja',       @hashPass, 'Carlos Cajero Quispe',        @rolCaja),
        ('psicologo',  @hashPass, 'Dr. José Martínez Vargas',    @rolPsic);
END

-- Psicólogo demo
IF NOT EXISTS (SELECT 1 FROM Psicologo WHERE colegiatura = 'CPP-12345')
BEGIN
    DECLARE @espId    INT = (SELECT TOP 1 id FROM Especialidad WHERE nombre = 'Psicología Clínica');
    DECLARE @usrPsic  INT = (SELECT id FROM Usuario WHERE username = 'psicologo');
    INSERT INTO Psicologo (usuario_id, nombres, apellidos, colegiatura, telefono, email, especialidad_id) VALUES
        (@usrPsic, 'José', 'Martínez Vargas', 'CPP-12345', '987654321', 'jose.martinez@clinica.pe', @espId);

    DECLARE @psicId INT = SCOPE_IDENTITY();
    -- Horario: Lun-Sab 08:00-19:00
    INSERT INTO HorarioPsicologo (psicologo_id, dia_semana, hora_inicio, hora_fin) VALUES
        (@psicId, 1, '08:00', '19:00'),
        (@psicId, 2, '08:00', '19:00'),
        (@psicId, 3, '08:00', '19:00'),
        (@psicId, 4, '08:00', '19:00'),
        (@psicId, 5, '08:00', '19:00'),
        (@psicId, 6, '08:00', '19:00');
END

-- Paciente demo
IF NOT EXISTS (SELECT 1 FROM Paciente WHERE dni = '76543210')
INSERT INTO Paciente (numero_historia, dni, nombres, apellidos, fecha_nacimiento, sexo, telefono, email) VALUES
    ('HC-0001', '76543210', 'María', 'González Pérez', '1990-05-15', 'F', '999888777', 'maria.gonzalez@email.com'),
    ('HC-0002', '12345678', 'Juan',  'Pérez Torres',   '1985-03-22', 'M', '911222333', 'juan.perez@email.com');

PRINT 'Base de datos ClinicaPsicologia inicializada correctamente.';
GO
