-- ============================================================
-- AUDITORIA DE INTEGRIDAD: PRIMARY KEY y FOREIGN KEY
-- Compara el esquema real de la base contra lo declarado en database/init.sql.
-- Solo lectura, no modifica nada. Seguro de correr en cualquier momento.
-- Contexto: ver CLAUDE.md, seccion "Incidente resuelto: desincronizacion
-- de esquema en Neon".
-- ============================================================

-- 1) PRIMARY KEY: toda tabla del esquema 'public' deberia tener una.
-- Excepcion conocida: 'sysdiagrams' es residuo del Database Diagram de SQL
-- Server Management Studio (de antes de la migracion a Postgres). No es
-- parte del esquema de la aplicacion, no tiene PK por diseno, y se ignora.
SELECT
    c.relname AS tabla,
    EXISTS (
        SELECT 1 FROM pg_constraint con
        WHERE con.conrelid = c.oid AND con.contype = 'p'
    ) AS tiene_primary_key
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE c.relkind = 'r'
  AND n.nspname = 'public'
ORDER BY tabla;

-- 2) FOREIGN KEY: las declaradas en init.sql vs. las que existen realmente.
-- Si alguna tabla referenciada aun no existe, esa fila simplemente sale
-- como 'FALTA' (no da error). Correr esto siempre despues de cualquier
-- operacion que suelte y reponga constraints -- no asumir el resultado.
WITH esperadas (tabla, columna, tabla_referenciada, columna_referenciada) AS (
    VALUES
        ('usuario',            'rol_id',                 'rol',                'id'),
        ('psicologo',          'usuario_id',             'usuario',            'id'),
        ('psicologo',          'especialidad_id',        'especialidad',       'id'),
        ('horariopsicologo',   'psicologo_id',           'psicologo',          'id'),
        ('cita',               'paciente_id',            'paciente',           'id'),
        ('cita',               'psicologo_id',           'psicologo',          'id'),
        ('cita',               'especialidad_id',        'especialidad',       'id'),
        ('cita',               'ticket_id',              'ticket',             'id'),
        ('cita',               'creado_por',             'usuario',            'id'),
        ('deuda',              'paciente_id',            'paciente',           'id'),
        ('deuda',              'cita_id',                'cita',               'id'),
        ('comprobantepago',    'deuda_id',               'deuda',              'id'),
        ('procesoterapeutico', 'paciente_id',            'paciente',           'id'),
        ('procesoterapeutico', 'psicologo_id',           'psicologo',          'id'),
        ('entrevistainicial',  'proceso_terapeutico_id', 'procesoterapeutico', 'id'),
        ('sesion',             'cita_id',                'cita',               'id'),
        ('sesion',             'proceso_terapeutico_id', 'procesoterapeutico', 'id'),
        ('sesion',             'registrado_por',         'usuario',            'id')
),
reales AS (
    SELECT
        tc.table_name    AS tabla,
        kcu.column_name  AS columna,
        ccu.table_name   AS tabla_referenciada,
        ccu.column_name  AS columna_referenciada,
        tc.constraint_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
        ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema
    JOIN information_schema.constraint_column_usage ccu
        ON tc.constraint_name = ccu.constraint_name AND tc.table_schema = ccu.table_schema
    WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'public'
)
SELECT
    COALESCE(e.tabla, r.tabla)                               AS tabla,
    COALESCE(e.columna, r.columna)                           AS columna,
    COALESCE(e.tabla_referenciada, r.tabla_referenciada)     AS tabla_referenciada,
    COALESCE(e.columna_referenciada, r.columna_referenciada) AS columna_referenciada,
    CASE
        WHEN e.tabla IS NULL THEN 'INESPERADA (no esta en init.sql)'
        WHEN r.tabla IS NULL THEN 'FALTA'
        ELSE 'OK'
    END AS estado,
    r.constraint_name AS nombre_constraint_actual
FROM esperadas e
FULL OUTER JOIN reales r
    ON r.tabla = e.tabla
    AND r.columna = e.columna
    AND r.tabla_referenciada = e.tabla_referenciada
    AND r.columna_referenciada = e.columna_referenciada
ORDER BY tabla, columna;
