IF DB_ID(N'ClinicaPsicologia') IS NULL
BEGIN
    CREATE DATABASE [ClinicaPsicologia];
END;
GO

USE [ClinicaPsicologia];
GO

IF OBJECT_ID(N'dbo.rol', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.rol (
        id          INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        nombre      VARCHAR(50) NOT NULL UNIQUE,
        descripcion VARCHAR(200) NULL
    );
END;
GO

IF OBJECT_ID(N'dbo.usuario', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.usuario (
        id              INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        username        VARCHAR(50) NOT NULL UNIQUE,
        password_hash   VARCHAR(255) NOT NULL,
        nombre_completo VARCHAR(150) NOT NULL,
        rol_id          INT NOT NULL,
        activo          BIT NOT NULL CONSTRAINT DF_usuario_activo DEFAULT (1),
        creado_en       DATETIME2 NOT NULL CONSTRAINT DF_usuario_creado_en DEFAULT (SYSDATETIME()),
        email           VARCHAR(150) NOT NULL UNIQUE,
        CONSTRAINT FK_usuario_rol FOREIGN KEY (rol_id) REFERENCES dbo.rol(id)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.rol WHERE nombre = 'ADMIN')
    INSERT INTO dbo.rol (nombre, descripcion) VALUES ('ADMIN', 'Administrador del sistema');
IF NOT EXISTS (SELECT 1 FROM dbo.rol WHERE nombre = 'RECEPCION')
    INSERT INTO dbo.rol (nombre, descripcion) VALUES ('RECEPCION', 'Personal de recepcion');
IF NOT EXISTS (SELECT 1 FROM dbo.rol WHERE nombre = 'CAJA')
    INSERT INTO dbo.rol (nombre, descripcion) VALUES ('CAJA', 'Cajero de consultorios');
IF NOT EXISTS (SELECT 1 FROM dbo.rol WHERE nombre = 'PSICOLOGO')
    INSERT INTO dbo.rol (nombre, descripcion) VALUES ('PSICOLOGO', 'Psicologo tratante');
IF NOT EXISTS (SELECT 1 FROM dbo.rol WHERE nombre = 'ANFITRIONA')
    INSERT INTO dbo.rol (nombre, descripcion) VALUES ('ANFITRIONA', 'Personal de bienvenida y tickets');
IF NOT EXISTS (SELECT 1 FROM dbo.rol WHERE nombre = 'ENFERMERA')
    INSERT INTO dbo.rol (nombre, descripcion) VALUES ('ENFERMERA', 'Enfermera de piso');
GO

DECLARE @hash VARCHAR(255) = '$2a$10$2OrRLP6u9I0Txwl8gNJmPeQOI4BSAPZt865HXfsSwm1tuVQmshkWm';

IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE username = 'admin')
    INSERT INTO dbo.usuario (username, password_hash, nombre_completo, rol_id, email)
    SELECT 'admin', @hash, 'Administrador del Sistema', id, 'admin@clinica.local' FROM dbo.rol WHERE nombre = 'ADMIN';
IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE username = 'recepcion')
    INSERT INTO dbo.usuario (username, password_hash, nombre_completo, rol_id, email)
    SELECT 'recepcion', @hash, 'Maria Recepcionista Lopez', id, 'recepcion@clinica.local' FROM dbo.rol WHERE nombre = 'RECEPCION';
IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE username = 'caja')
    INSERT INTO dbo.usuario (username, password_hash, nombre_completo, rol_id, email)
    SELECT 'caja', @hash, 'Carlos Cajero Quispe', id, 'caja@clinica.local' FROM dbo.rol WHERE nombre = 'CAJA';
IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE username = 'psicologo')
    INSERT INTO dbo.usuario (username, password_hash, nombre_completo, rol_id, email)
    SELECT 'psicologo', @hash, 'Dr. Jose Martinez Vargas', id, 'psicologo@clinica.local' FROM dbo.rol WHERE nombre = 'PSICOLOGO';
IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE username = 'psicologo2')
    INSERT INTO dbo.usuario (username, password_hash, nombre_completo, rol_id, email)
    SELECT 'psicologo2', @hash, 'Rosa Quispe Flores', id, 'rosa.quispe@clinica.local' FROM dbo.rol WHERE nombre = 'PSICOLOGO';
IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE username = 'anfitriona')
    INSERT INTO dbo.usuario (username, password_hash, nombre_completo, rol_id, email)
    SELECT 'anfitriona', @hash, 'Ana Anfitriona Ramos', id, 'anfitriona@clinica.local' FROM dbo.rol WHERE nombre = 'ANFITRIONA';
IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE username = 'enfermera')
    INSERT INTO dbo.usuario (username, password_hash, nombre_completo, rol_id, email)
    SELECT 'enfermera', @hash, 'Elena Enfermera Rojas', id, 'enfermera@clinica.local' FROM dbo.rol WHERE nombre = 'ENFERMERA';
GO
