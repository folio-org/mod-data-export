CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_folio_instance_all
AS SELECT inst.id, jsonb FROM ${myuniversity}_mod_data_export.v_instance inst
   WHERE jsonb ->> 'source' = 'FOLIO';

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_folio_instance_all_non_deleted
AS SELECT inst.id, jsonb FROM ${myuniversity}_mod_data_export.v_instance inst
   WHERE jsonb ->> 'source' = 'FOLIO' AND
       ((jsonb ->> 'discoverySuppress' = 'true' AND (jsonb ->> 'staffSuppress' IS NULL OR jsonb ->> 'staffSuppress' = 'false')) OR
       (jsonb ->> 'discoverySuppress' IS NULL OR jsonb ->> 'discoverySuppress' = 'false'));
