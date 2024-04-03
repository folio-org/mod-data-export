CREATE OR REPLACE VIEW v_marc_records_lb
    AS SELECT id, content, external_id, record_type::text, state::text, leader_record_status, suppress_discovery, generation
    FROM ${myuniversity}_mod_source_record_storage.records_lb records_lb
    JOIN ${myuniversity}_mod_source_record_storage.marc_records_lb using(id);
