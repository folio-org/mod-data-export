CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE OR REPLACE PROCEDURE slice_authorities_all_ids(jobExecutionId text, fileLocation text, sliceSize int)
LANGUAGE plpgsql
AS $$
BEGIN

 WITH RankedRows as (select rec.external_id id,(ROW_NUMBER() OVER (ORDER BY rec.external_id)) - 1 row_num
    from ${myuniversity}_mod_data_export.v_authority_all rec order by id),
 IndexedRows as (select id, (row_num / sliceSize) group_index, (row_num % sliceSize) local_index from RankedRows),
 GroupedRows as (select id,
						group_index,
						local_index,
						max(local_index) over (partition by group_index) max_local_index,
						min(local_index) over (partition by group_index) min_local_index
				 from IndexedRows),
 SlicedInstancesIds as (
            select group_index,
            max(max_local_index)                                               max_local_index,
            min(min_local_index)                                               min_local_index,
            (ARRAY_AGG(id) FILTER (WHERE local_index = min_local_index))[1] AS min_id,
            (ARRAY_AGG(id) FILTER (WHERE local_index = max_local_index))[1] AS max_id
          from GroupedRows group by group_index)

insert into job_execution_export_files(id, job_execution_id, file_location, from_id, to_id, status)
    select gen_random_uuid() as id,
jobExecutionId::uuid as job_execution_id,format(fileLocation, min_id, max_id)
    as file_location, min_id as from_id, max_id as to_id, 'SCHEDULED' as status from SlicedInstancesIds;

END;
$$;
