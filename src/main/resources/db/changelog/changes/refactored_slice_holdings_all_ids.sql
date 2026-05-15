CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE OR REPLACE PROCEDURE slice_holdings_all_ids(jobExecutionId text, fileLocation text, sliceSize int)
LANGUAGE plpgsql
AS $$
BEGIN
    -- Validate sliceSize to avoid division by zero or negative values
    IF sliceSize IS NULL OR sliceSize <= 0 THEN
        RAISE EXCEPTION 'sliceSize must be a positive integer. Provided: %', sliceSize;
    END IF;

    -- Validate jobExecutionId can be cast to uuid early for clearer error
    PERFORM jobExecutionId::uuid; -- will raise if invalid

    WITH OrderedIds AS (
        SELECT DISTINCT rec.id
        FROM ${myuniversity}_mod_inventory_storage.holdings_record rec
        ORDER BY rec.id
    ),
    RankedRows AS (
        SELECT id, (ROW_NUMBER() OVER (ORDER BY id) - 1) AS row_num
        FROM OrderedIds
    ),
    Sliced AS (
        SELECT (row_num / sliceSize) AS group_index,
               MIN(id::text)::uuid AS min_id,
               MAX(id::text)::uuid AS max_id
        FROM RankedRows
        GROUP BY (row_num / sliceSize)
        ORDER BY group_index
    )
    INSERT INTO job_execution_export_files(id, job_execution_id, file_location, from_id, to_id, status)
    SELECT gen_random_uuid() AS id,
           jobExecutionId::uuid AS job_execution_id,
           format(fileLocation, min_id, max_id) AS file_location,
           min_id AS from_id,
           max_id AS to_id,
           'SCHEDULED' AS status
    FROM Sliced;
END;
$$;
