CREATE OR REPLACE VIEW v_audit_instance
    AS SELECT uuid(jsonb->'record'->>'id') as id, jsonb->'record'->>'title' as title, jsonb->'record'->>'hrid' as hrid FROM ${myuniversity}_mod_inventory_storage.audit_instance;
