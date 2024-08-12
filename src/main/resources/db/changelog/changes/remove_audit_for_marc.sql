-- marc instance views:

-- exclude deleted from inventory
CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_marc_instance_all_non_deleted
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_all_marc_non_deleted
    WHERE record_type = 'MARC_BIB';

-- include deleted from inventory
CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_instance_all_marc_deleted
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_all_marc_deleted
    WHERE record_type = 'MARC_BIB';

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_instance_all_marc_deleted_custom_instance_profile
    AS SELECT DISTINCT inst.id, inst.jsonb FROM ${myuniversity}_mod_data_export.v_instance inst
    JOIN ${myuniversity}_mod_data_export.v_all_marc_deleted marc_del
    ON inst.id = marc_del.external_id;

-- marc holdings views:

-- exclude deleted from inventory
CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_marc_holdings_all_non_deleted
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_all_marc_non_deleted
    WHERE record_type = 'MARC_HOLDING';

-- include deleted from inventory
CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_holdings_all_marc_deleted
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_all_marc_deleted
    WHERE record_type = 'MARC_HOLDING';

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_holdings_all_marc_deleted_custom_profile
    AS SELECT holds.id, holds.jsonb, holds.instance_id FROM ${myuniversity}_mod_data_export.v_holdings_record holds
    JOIN ${myuniversity}_mod_data_export.v_all_marc_deleted marc_del
    ON holds.id = marc_del.external_id;

-- all dependent marc instance views:

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_marc_instance_all_non_deleted_non_suppressed
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_marc_instance_all_non_deleted
    WHERE suppress_discovery = false;

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_instance_all_marc_deleted_not_suppressed
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_instance_all_marc_deleted
    WHERE suppress_discovery = false;

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_instance_all_marc_deleted_not_suppressed_custom_instance_profile
    AS SELECT DISTINCT id, jsonb FROM ${myuniversity}_mod_data_export.v_instance_all_marc_deleted_custom_instance_profile
    WHERE (jsonb ->> 'discoverySuppress' = 'false' OR jsonb ->> 'discoverySuppress' IS NULL);

-- all dependent marc holdings views:

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_marc_holdings_all_non_deleted_non_suppressed
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_marc_holdings_all_non_deleted
    WHERE suppress_discovery = false;

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_holdings_all_marc_deleted_not_suppressed
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_holdings_all_marc_deleted
    WHERE suppress_discovery = false;

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_holdings_all_marc_deleted_not_suppressed_custom_profile
    AS SELECT * FROM ${myuniversity}_mod_data_export.v_holdings_all_marc_deleted_custom_profile holds
    WHERE (holds.jsonb ->> 'discoverySuppress' = 'false' OR holds.jsonb ->> 'discoverySuppress' IS NULL);
