DROP TRIGGER IF EXISTS trg_clinical_session_immutable ON clinical_session;
DROP FUNCTION IF EXISTS reject_clinical_session_update();

CREATE OR REPLACE FUNCTION reject_clinical_session_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Las sesiones clinicas son inmutables y solo admiten inserciones';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_clinical_session_append_only
BEFORE UPDATE OR DELETE ON clinical_session
FOR EACH ROW EXECUTE FUNCTION reject_clinical_session_mutation();
