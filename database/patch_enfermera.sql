-- ============================================================
-- PATCH NEON: Rol ENFERMERA y estado EN_PISO
-- Ejecutar una sola vez sobre la base existente.
-- Password demo para usuario enfermera: 123
-- ============================================================

DO $$
DECLARE
    hash_pass TEXT := '$2a$10$2OrRLP6u9I0Txwl8gNJmPeQOI4BSAPZt865HXfsSwm1tuVQmshkWm';
    enfermera_rol_id INTEGER;
    enfermera_usuario_id INTEGER;
BEGIN
    SELECT id
    INTO enfermera_rol_id
    FROM rol
    WHERE nombre = 'ENFERMERA';

    IF enfermera_rol_id IS NULL THEN
        SELECT COALESCE(MAX(id), 0) + 1
        INTO enfermera_rol_id
        FROM rol;

        INSERT INTO rol (id, nombre, descripcion)
        VALUES (enfermera_rol_id, 'ENFERMERA', 'Enfermera de piso');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM usuario WHERE username = 'enfermera') THEN
        SELECT COALESCE(MAX(id), 0) + 1
        INTO enfermera_usuario_id
        FROM usuario;

        INSERT INTO usuario (id, username, password_hash, nombre_completo, rol_id, activo, creado_en, email)
        VALUES (
            enfermera_usuario_id,
            'enfermera',
            hash_pass,
            'Elena Enfermera Rojas',
            enfermera_rol_id,
            TRUE,
            CURRENT_TIMESTAMP,
            'enfermera@clinica.local'
        );
    END IF;
END $$;

DO $$
DECLARE
    nombre_constraint TEXT;
BEGIN
    SELECT con.conname
    INTO nombre_constraint
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY(con.conkey)
    WHERE rel.relname = 'cita'
      AND con.contype = 'c'
      AND att.attname = 'estado'
    LIMIT 1;

    IF nombre_constraint IS NOT NULL THEN
        EXECUTE format('ALTER TABLE cita DROP CONSTRAINT %I', nombre_constraint);
    END IF;
END $$;

ALTER TABLE cita
ADD CONSTRAINT cita_estado_check
CHECK (estado IN ('PENDIENTE_PAGO', 'PAGADA', 'EN_PISO', 'EN_CONSULTA', 'ATENDIDA', 'CANCELADA'));
