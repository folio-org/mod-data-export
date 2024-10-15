CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_folio_instance_all
AS SELECT inst.id, jsonb FROM ${myuniversity}_mod_data_export.v_instance inst
   WHERE jsonb ->> 'source' = 'FOLIO';

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_folio_all_deleted_non_suppressed
AS SELECT inst.id, jsonb FROM ${myuniversity}_mod_data_export.v_instance inst
    WHERE jsonb ->> 'source' = 'FOLIO' AND
          ((jsonb ->> 'discoverySuppress' = 'true' AND jsonb ->> 'staffSuppress' = 'true') OR
          (jsonb ->> 'discoverySuppress' IS NULL OR jsonb ->> 'discoverySuppress' = 'false'));

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_folio_all_non_deleted_suppressed
AS SELECT inst.id, jsonb FROM ${myuniversity}_mod_data_export.v_instance inst
   WHERE jsonb ->> 'source' = 'FOLIO' AND
         ((jsonb ->> 'discoverySuppress' = 'true' AND (jsonb ->> 'staffSuppress' IS NULL OR jsonb ->> 'staffSuppress' = 'false')) OR
         (jsonb ->> 'discoverySuppress' IS NULL OR jsonb ->> 'discoverySuppress' = 'false'));

CREATE OR REPLACE VIEW ${myuniversity}_mod_data_export.v_folio_instance_all_non_deleted_non_suppressed
AS SELECT inst.id, jsonb FROM ${myuniversity}_mod_data_export.v_instance inst
   WHERE jsonb ->> 'source' = 'FOLIO' AND
       (jsonb ->> 'discoverySuppress' IS NULL OR jsonb ->> 'discoverySuppress' = 'false');
