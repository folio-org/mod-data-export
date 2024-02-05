CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_instance_all
    AS SELECT inst.id, jsonb FROM ${myuniversity}_mod_data_export.v_instance inst;

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_authority_all
    AS SELECT id, content, external_id, record_type, suppress_discovery, state, leader_record_status
    FROM ${myuniversity}_mod_source_record_storage.records_lb records_lb
    JOIN ${myuniversity}_mod_source_record_storage.marc_records_lb using(id)
    WHERE records_lb.record_type = 'MARC_AUTHORITY';

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_holdings_all_deleted
    AS SELECT id, jsonb FROM ${myuniversity}_mod_inventory_storage.audit_holdings_record audit_holds;

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_instance_all_deleted
    AS SELECT id, jsonb FROM ${myuniversity}_mod_inventory_storage.audit_instance audit_inst;

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_holdings_all
    AS SELECT holds.id, holds.jsonb, holds.instance_id
    FROM ${myuniversity}_mod_data_export.v_holdings_record holds;