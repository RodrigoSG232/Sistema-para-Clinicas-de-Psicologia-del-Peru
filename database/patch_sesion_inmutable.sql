-- ============================================================
-- PATCH NEON: Inmutabilidad de Sesion (HU19)
-- Ejecutar una sola vez sobre la base existente. Re-ejecutable sin error.
-- Bloquea UPDATE y DELETE sobre 'sesion' a nivel de base de datos, sin
-- importar el camino por el que se intente (backend, SQL manual, otra app).
-- Si hace falta corregir un error, se registra una nota nueva que
-- referencie la sesion original, no se edita ni se borra esta.
-- ============================================================

CREATE OR REPLACE FUNCTION bloquear_modificacion_sesion()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Las sesiones no se pueden modificar ni eliminar una vez guardadas (sesion id=%). Para corregir un error, registre una nota nueva que referencie esta sesion.', OLD.id;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sesion_inmutable ON sesion;

CREATE TRIGGER trg_sesion_inmutable
BEFORE UPDATE OR DELETE ON sesion
FOR EACH ROW
EXECUTE FUNCTION bloquear_modificacion_sesion();
