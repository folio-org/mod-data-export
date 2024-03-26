CREATE OR REPLACE VIEW v_audit_instance
    AS SELECT id, jsonb FROM ${myuniversity}_mod_inventory_storage.audit_instance;
