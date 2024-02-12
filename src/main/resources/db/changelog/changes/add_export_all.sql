CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_all_marc_deleted
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_marc_records_lb
    WHERE (state = 'ACTUAL' AND leader_record_status = 'd' OR state = 'DELETED');

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_all_marc_non_deleted
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_marc_records_lb
    WHERE state = 'ACTUAL' AND leader_record_status != 'd';

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_authority_all
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_marc_records_lb
    WHERE record_type = 'MARC_AUTHORITY';


-- onlyNonDeleted, suppressedFromDiscovery = true
CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_folio_instance_all_non_deleted
    AS SELECT inst.id, jsonb FROM ${myuniversity}_mod_data_export.v_instance inst
    WHERE jsonb ->> 'source' = 'FOLIO';

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_folio_holdings_all_non_deleted
    AS SELECT holds.id, holds.jsonb, holds.instanceid as instance_id
    FROM ${myuniversity}_mod_inventory_storage.holdings_record holds
    JOIN ${myuniversity}_mod_inventory_storage.holdings_records_source src
    ON holds.jsonb ->> 'sourceId' = src.id::text
    AND src.jsonb ->> 'name' = 'FOLIO';

-- exclude deleted from inventory
CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_marc_instance_all_non_deleted
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_all_marc_non_deleted
    WHERE record_type = 'MARC_BIB'
    AND external_id NOT IN (
        SELECT uuid(jsonb -> 'record' ->> 'id') FROM ${myuniversity}_mod_inventory_storage.audit_instance);

-- exclude deleted from inventory
CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_marc_holdings_all_non_deleted
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_all_marc_non_deleted
    WHERE record_type = 'MARC_HOLDING'
    AND external_id NOT IN (
        SELECT uuid(jsonb -> 'record' ->> 'id') FROM ${myuniversity}_mod_inventory_storage.audit_holdings_record);

-- onlyNonDeleted, suppressedFromDiscovery = false
CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_folio_instance_all_non_deleted_non_suppressed
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_folio_instance_all_non_deleted
    WHERE jsonb ->> 'discoverySuppress' is null OR jsonb ->> 'discoverySuppress' = 'false';

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_folio_holdings_all_non_deleted_non_suppressed
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_folio_holdings_all_non_deleted
    WHERE jsonb ->> 'discoverySuppress' is null OR jsonb ->> 'discoverySuppress' = 'false';

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_marc_instance_all_non_deleted_non_suppressed
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_marc_instance_all_non_deleted
    WHERE suppress_discovery = false;

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_marc_holdings_all_non_deleted_non_suppressed
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_marc_holdings_all_non_deleted
    WHERE suppress_discovery = false;

-- onlyDeleted, suppressedFromDiscovery = true
CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_instance_all_folio_deleted
    AS SELECT uuid(jsonb -> 'record' ->> 'id') as id, to_jsonb(jsonb -> 'record') as jsonb
    FROM ${myuniversity}_mod_inventory_storage.audit_instance audit_inst
    WHERE jsonb -> 'record' ->> 'source' = 'FOLIO';

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_holdings_all_folio_deleted
    AS SELECT uuid(audit_holds.jsonb -> 'record' ->> 'id') as id, to_jsonb(audit_holds.jsonb -> 'record') as jsonb, uuid(audit_holds.jsonb -> 'record' ->> 'instanceId') as instance_id
    FROM ${myuniversity}_mod_inventory_storage.audit_holdings_record audit_holds
    JOIN ${myuniversity}_mod_inventory_storage.holdings_records_source src
    ON audit_holds.jsonb -> 'record' ->> 'sourceId' = src.id::text
    AND src.jsonb ->> 'name' = 'FOLIO';

-- include deleted from inventory
CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_instance_all_marc_deleted
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_all_marc_deleted
    WHERE record_type = 'MARC_BIB'
    UNION
    SELECT * FROM ${myuniversity}_mod_data_export.v_marc_records_lb
    WHERE external_id IN (
        SELECT uuid(jsonb -> 'record' ->> 'id') FROM ${myuniversity}_mod_inventory_storage.audit_instance);

-- include deleted from inventory
CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_holdings_all_marc_deleted
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_all_marc_deleted
    WHERE record_type = 'MARC_HOLDING'
    UNION
    SELECT * FROM ${myuniversity}_mod_data_export.v_marc_records_lb
    WHERE external_id IN (
        SELECT uuid(jsonb -> 'record' ->> 'id') FROM ${myuniversity}_mod_inventory_storage.audit_holdings_record);

-- onlyDeleted, suppressedFromDiscovery = false
CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_instance_all_folio_deleted_not_suppressed
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_instance_all_folio_deleted
    WHERE (jsonb ->> 'discoverySuppress' = 'false'
    OR jsonb ->> 'discoverySuppress' IS NULL);

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_holdings_all_folio_deleted_not_suppressed
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_holdings_all_folio_deleted
    WHERE jsonb ->> 'discoverySuppress' = 'false'
    OR jsonb ->> 'discoverySuppress' IS NULL;

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_instance_all_marc_deleted_not_suppressed
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_instance_all_marc_deleted
    WHERE suppress_discovery = false;

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_holdings_all_marc_deleted_not_suppressed
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_holdings_all_marc_deleted
    WHERE suppress_discovery = false;
