CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_instance_all
    AS SELECT inst.id, jsonb FROM ${myuniversity}_mod_data_export.v_instance inst
    UNION
    SELECT uuid(audit.jsonb -> 'record' ->> 'id'), audit.jsonb -> 'record'
    FROM ${myuniversity}_mod_inventory_storage.audit_instance audit;;

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_authority_all
    AS SELECT id, content, external_id, record_type, suppress_discovery, state, leader_record_status
    FROM ${myuniversity}_mod_source_record_storage.records_lb records_lb
    JOIN ${myuniversity}_mod_source_record_storage.marc_records_lb using(id)
    WHERE records_lb.record_type = 'MARC_AUTHORITY';

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_holdings_all_deleted
    AS SELECT audit_holds.id, audit_holds.jsonb
    FROM ${myuniversity}_mod_inventory_storage.audit_holdings_record audit_holds
    JOIN ${myuniversity}_mod_inventory_storage.holdings_records_source src
    ON audit_holds.jsonb -> 'record' ->> 'sourceId' = src.id::text
    AND src.jsonb ->> 'name' = 'FOLIO';

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_instance_all_deleted
    AS SELECT id, jsonb FROM ${myuniversity}_mod_inventory_storage.audit_instance audit_inst
    WHERE jsonb -> 'record' ->> 'source' = 'FOLIO';

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_holdings_all
    AS SELECT holds.id, holds.jsonb, holds.instance_id
    FROM ${myuniversity}_mod_data_export.v_holdings_record holds
    UNION
    SELECT uuid(audit.jsonb -> 'record' ->> 'id'), audit.jsonb -> 'record', uuid(audit.jsonb -> 'record' ->> 'instanceId')
    FROM ${myuniversity}_mod_inventory_storage.audit_holdings_record audit;
