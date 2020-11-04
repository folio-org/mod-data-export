--a Update fieldId fields in mapping profile transformations
UPDATE ${myuniversity}_${mymodule}.mapping_profiles as profiles
SET jsonb = jsonb_set(jsonb, '{transformations}', (
  SELECT jsonb_agg(jsonb_set(transformations.value, '{fieldId}', to_jsonb(
     CASE
        WHEN transformations.value ->> 'recordType' = 'HOLDINGS' AND transformations.value ->> 'fieldId' NOT LIKE '%holdings.%'
        THEN concat('holdings.', transformations.value ->> 'fieldId')
        WHEN transformations.value ->> 'recordType' = 'ITEM' AND transformations.value ->> 'fieldId' NOT LIKE '%item.%'
        THEN concat('item.', transformations.value ->> 'fieldId')
        WHEN transformations.value ->> 'recordType' = 'INSTANCE' AND transformations.value ->> 'fieldId' NOT LIKE '%instance.%'
        THEN concat('instance.', transformations.value ->> 'fieldId')
        ELSE transformations.value ->> 'fieldId'
       END
    )))
  FROM ${myuniversity}_${mymodule}.mapping_profiles, jsonb_array_elements(jsonb -> 'transformations') AS transformations
  WHERE ${myuniversity}_${mymodule}.mapping_profiles.id = profiles.id
  GROUP BY id
))
WHERE profiles.jsonb ->> 'transformations' IS NOT null;

--a Update path fields in mapping profile transformations for item record type
UPDATE ${myuniversity}_${mymodule}.mapping_profiles as profiles
SET jsonb = jsonb_set(jsonb, '{transformations}', (
  SELECT jsonb_agg(jsonb_set(transformations.value, '{path}', to_jsonb(
    CASE
       WHEN transformations.value ->> 'recordType' = 'ITEM' AND transformations.value ->> 'path' NOT LIKE '%holdings[*]%'
       THEN regexp_replace(transformations.value ->> 'path', '[$.]', '$.holdings[*]')
       ELSE transformations.value ->> 'path'
    END
    )))
  FROM ${myuniversity}_${mymodule}.mapping_profiles, jsonb_array_elements(jsonb -> 'transformations') AS transformations
  WHERE ${myuniversity}_${mymodule}.mapping_profiles.id = profiles.id
  GROUP BY id
))
WHERE profiles.jsonb ->> 'transformations' IS NOT null;
