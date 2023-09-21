UPDATE ${myuniversity}_${mymodule}.job_executions
SET jsonb = jsonb_set(jsonb, '{progress,failed}',
                      to_jsonb(CONCAT('{"otherFailed": ',
                                      to_jsonb((jsonb -> 'progress' ->> 'failed')::int),
                                               ', "duplicatedSrs": 0}')::json))
WHERE jsonb_typeof((jsonb -> 'progress' ->> 'failed')::jsonb) = 'number';