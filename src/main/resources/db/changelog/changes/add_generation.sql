CREATE OR REPLACE VIEW v_marc_records_lb
    AS SELECT id, content, external_id, record_type::text, state::text, leader_record_status, suppress_discovery, generation
    FROM ${myuniversity}_mod_source_record_storage.records_lb records_lb
    JOIN ${myuniversity}_mod_source_record_storage.marc_records_lb using(id);

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_all_marc_deleted
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_marc_records_lb
    WHERE (state = 'ACTUAL' AND leader_record_status = 'd' OR state = 'DELETED');

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_all_marc_non_deleted
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_marc_records_lb
    WHERE state = 'ACTUAL' AND leader_record_status != 'd';

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_authority_all
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_marc_records_lb
    WHERE record_type = 'MARC_AUTHORITY' AND state != 'OLD';


-- exclude deleted from inventory
CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_marc_instance_all_non_deleted
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_all_marc_non_deleted
    WHERE record_type = 'MARC_BIB'
    AND external_id NOT IN (
        SELECT uuid(jsonb -> 'record' ->> 'id') FROM ${myuniversity}_mod_inventory_storage.audit_instance);

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_marc_instance_all_non_deleted_custom_profile
    AS SELECT DISTINCT inst.id, inst.jsonb FROM ${myuniversity}_mod_data_export.v_instance inst
    JOIN ${myuniversity}_mod_data_export.v_all_marc_non_deleted marc_non_del
    ON inst.id = marc_non_del.external_id
    WHERE jsonb ->> 'source' = 'MARC';

-- exclude deleted from inventory
CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_marc_holdings_all_non_deleted
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_all_marc_non_deleted
    WHERE record_type = 'MARC_HOLDING'
    AND external_id NOT IN (
        SELECT uuid(jsonb -> 'record' ->> 'id') FROM ${myuniversity}_mod_inventory_storage.audit_holdings_record);

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_marc_holdings_all_non_deleted_custom_profile
    AS SELECT holds.id, holds.jsonb, holds.instance_id FROM ${myuniversity}_mod_data_export.v_holdings_record holds
    JOIN ${myuniversity}_mod_data_export.v_all_marc_non_deleted marc_non_del
    ON holds.id = marc_non_del.external_id AND marc_non_del.record_type = 'MARC_HOLDING';

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_marc_instance_all_non_deleted_non_suppressed
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_marc_instance_all_non_deleted
    WHERE suppress_discovery = false;

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_marc_instance_all_non_deleted_non_suppressed_custom_instance_profile
    AS SELECT DISTINCT inst.id, inst.jsonb FROM ${myuniversity}_mod_data_export.v_marc_instance_all_non_deleted_custom_profile inst
    WHERE (inst.jsonb ->> 'discoverySuppress' = 'false' OR inst.jsonb ->> 'discoverySuppress' IS NULL);

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_marc_holdings_all_non_deleted_non_suppressed
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_marc_holdings_all_non_deleted
    WHERE suppress_discovery = false;

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_marc_holdings_all_non_deleted_non_suppressed_custom_profile
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_marc_holdings_all_non_deleted_custom_profile holds
    WHERE (holds.jsonb ->> 'discoverySuppress' = 'false' OR holds.jsonb ->> 'discoverySuppress' IS NULL);

-- include deleted from inventory
CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_instance_all_marc_deleted
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_all_marc_deleted
    WHERE record_type = 'MARC_BIB'
    UNION
    SELECT * FROM ${myuniversity}_mod_data_export.v_marc_records_lb
    WHERE external_id IN (
        SELECT uuid(jsonb -> 'record' ->> 'id') FROM ${myuniversity}_mod_inventory_storage.audit_instance);

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_instance_all_marc_deleted_custom_instance_profile
    AS SELECT DISTINCT inst.id, inst.jsonb FROM ${myuniversity}_mod_data_export.v_instance inst
    JOIN ${myuniversity}_mod_data_export.v_all_marc_deleted marc_del
    ON inst.id = marc_del.external_id
    UNION
    SELECT DISTINCT uuid(audit_inst.jsonb -> 'record' ->> 'id') as id, to_jsonb(audit_inst.jsonb -> 'record') as jsonb
    FROM ${myuniversity}_mod_inventory_storage.audit_instance audit_inst
    WHERE audit_inst.jsonb -> 'record' ->> 'source' = 'MARC';

-- include deleted from inventory
CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_holdings_all_marc_deleted
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_all_marc_deleted
    WHERE record_type = 'MARC_HOLDING'
    UNION
    SELECT * FROM ${myuniversity}_mod_data_export.v_marc_records_lb
    WHERE external_id IN (
        SELECT uuid(jsonb -> 'record' ->> 'id') FROM ${myuniversity}_mod_inventory_storage.audit_holdings_record);

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_holdings_all_marc_deleted_custom_profile
    AS SELECT holds.id, holds.jsonb, holds.instance_id FROM ${myuniversity}_mod_data_export.v_holdings_record holds
    JOIN ${myuniversity}_mod_data_export.v_all_marc_deleted marc_del
    ON holds.id = marc_del.external_id
    UNION
    SELECT uuid(audit_holds.jsonb -> 'record' ->> 'id') as id, to_jsonb(audit_holds.jsonb -> 'record') as jsonb,
        uuid(audit_holds.jsonb -> 'record' ->> 'instanceId') as instance_id
    FROM ${myuniversity}_mod_inventory_storage.audit_holdings_record audit_holds
    JOIN ${myuniversity}_mod_inventory_storage.holdings_records_source src
    ON audit_holds.jsonb -> 'record' ->> 'sourceId' = src.id::text
    AND src.jsonb ->> 'name' = 'MARC';

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_instance_all_marc_deleted_not_suppressed
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_instance_all_marc_deleted
    WHERE suppress_discovery = false;

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_instance_all_marc_deleted_not_suppressed_custom_instance_profile
    AS SELECT DISTINCT id, jsonb FROM ${myuniversity}_mod_data_export.v_instance_all_marc_deleted_custom_instance_profile
    WHERE (jsonb ->> 'discoverySuppress' = 'false' OR jsonb ->> 'discoverySuppress' IS NULL);

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_holdings_all_marc_deleted_not_suppressed
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_holdings_all_marc_deleted
    WHERE suppress_discovery = false;

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_holdings_all_marc_deleted_not_suppressed_custom_profile
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_holdings_all_marc_deleted_custom_profile holds
    WHERE (holds.jsonb ->> 'discoverySuppress' = 'false' OR holds.jsonb ->> 'discoverySuppress' IS NULL);

