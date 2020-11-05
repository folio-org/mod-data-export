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
WHERE profiles.jsonb ->> 'transformations' IS NOT null AND jsonb_array_length(profiles.jsonb -> 'transformations') > 0;

--a Update path fields for electronicAccess - change electronicAccess to electronic.access
UPDATE ${myuniversity}_${mymodule}.mapping_profiles as profiles
SET jsonb = jsonb_set(jsonb, '{transformations}', (
  SELECT jsonb_agg(jsonb_set(transformations.value, '{fieldId}', to_jsonb(
    CASE
     WHEN transformations.value ->> 'fieldId' LIKE '%electronicAccess%'
     THEN regexp_replace(transformations.value ->> 'fieldId', 'electronicAccess', 'electronic.access', 'q')
     ELSE transformations.value ->> 'fieldId'
    END
    )))
  FROM ${myuniversity}_${mymodule}.mapping_profiles, jsonb_array_elements(jsonb -> 'transformations') AS transformations
  WHERE ${myuniversity}_${mymodule}.mapping_profiles.id = profiles.id
  GROUP BY id
))
WHERE profiles.jsonb ->> 'transformations' IS NOT null AND jsonb_array_length(profiles.jsonb -> 'transformations') > 0;

--a Update fieldId fields in mapping profile transformations to lower case
UPDATE ${myuniversity}_${mymodule}.mapping_profiles as profiles
SET jsonb = jsonb_set(jsonb, '{transformations}', (
  SELECT jsonb_agg(jsonb_set(transformations.value, '{fieldId}', to_jsonb(LOWER(transformations.value ->> 'fieldId'))))
  FROM ${myuniversity}_${mymodule}.mapping_profiles, jsonb_array_elements(jsonb -> 'transformations') AS transformations
  WHERE ${myuniversity}_${mymodule}.mapping_profiles.id = profiles.id
  GROUP BY id
))
WHERE profiles.jsonb ->> 'transformations' IS NOT null AND jsonb_array_length(profiles.jsonb -> 'transformations') > 0;

--a Update path fields in mapping profile transformations for item record type - add holdings[*] at the beginning
UPDATE diku_mod_data_export.mapping_profiles as profiles
SET jsonb = jsonb_set(jsonb, '{transformations}', (
  SELECT jsonb_agg(jsonb_set(transformations.value, '{path}', to_jsonb(
    CASE
       WHEN transformations.value ->> 'recordType' = 'ITEM' AND transformations.value ->> 'path' NOT LIKE '%holdings[*]%'
       THEN regexp_replace(transformations.value ->> 'path', '$.', '$.holdings[*].', 'q')
       ELSE transformations.value ->> 'path'
    END
    )))
  FROM ${myuniversity}_${mymodule}.mapping_profiles, jsonb_array_elements(jsonb -> 'transformations') AS transformations
  WHERE ${myuniversity}_${mymodule}.mapping_profiles.id = profiles.id
  GROUP BY id
))
WHERE profiles.jsonb ->> 'transformations' IS NOT null AND jsonb_array_length(profiles.jsonb -> 'transformations') > 0;

--a Update fieldIds with default
UPDATE ${myuniversity}_${mymodule}.mapping_profiles as profiles
SET jsonb = jsonb_set(jsonb, '{transformations}', (
  SELECT jsonb_agg(jsonb_set(transformations.value, '{fieldId}', to_jsonb(
    CASE
       WHEN transformations.value ->> 'fieldId' LIKE '%linktext'
       THEN regexp_replace(transformations.value ->> 'fieldId', 'linktext', 'linktext.default')
	   	 WHEN transformations.value ->> 'fieldId' LIKE '%uri'
       THEN regexp_replace(transformations.value ->> 'fieldId', 'uri', 'uri.default')
	  	 WHEN transformations.value ->> 'fieldId' LIKE '%materialsspecification'
       THEN regexp_replace(transformations.value ->> 'fieldId', 'materialsspecification', 'materialsspecification.default')
	   	 WHEN transformations.value ->> 'fieldId' LIKE '%publicnote'
       THEN regexp_replace(transformations.value ->> 'fieldId', 'publicnote', 'publicnote.default')
       ELSE transformations.value ->> 'fieldId'
    END
    )))
  FROM ${myuniversity}_${mymodule}.mapping_profiles, jsonb_array_elements(jsonb -> 'transformations') AS transformations
  WHERE ${myuniversity}_${mymodule}.mapping_profiles.id = profiles.id
  GROUP BY id
))
WHERE profiles.jsonb ->> 'transformations' IS NOT null AND jsonb_array_length(profiles.jsonb -> 'transformations') > 0;

--a Update path for default electronic access - change electronicAccess[*] to electronicAccess[?(!(@.relationshipId) || @.relationshipId == null)]
UPDATE ${myuniversity}_${mymodule}.mapping_profiles as profiles
SET jsonb = jsonb_set(jsonb, '{transformations}', (
  SELECT jsonb_agg(jsonb_set(transformations.value, '{path}', to_jsonb(
    CASE
     WHEN transformations.value ->> 'fieldId' LIKE '%default' and transformations.value ->> 'path' LIKE '%electronicAccess[*]%'
     THEN regexp_replace(transformations.value ->> 'path', 'electronicAccess[*]', 'electronicAccess[?(!(@.relationshipId) || @.relationshipId == null)]', 'q')
     ELSE transformations.value ->> 'path'
    END
    )))
  FROM ${myuniversity}_${mymodule}.mapping_profiles, jsonb_array_elements(jsonb -> 'transformations') AS transformations
  WHERE ${myuniversity}_${mymodule}.mapping_profiles.id = profiles.id
  GROUP BY id
))
WHERE profiles.jsonb ->> 'transformations' IS NOT null AND jsonb_array_length(profiles.jsonb -> 'transformations') > 0;

--a Update location fieldIds
UPDATE ${myuniversity}_${mymodule}.mapping_profiles as profiles
SET jsonb = jsonb_set(jsonb, '{transformations}', (
  SELECT jsonb_agg(jsonb_set(transformations.value, '{fieldId}', to_jsonb(
    CASE
     WHEN transformations.value ->> 'fieldId' LIKE '%permanentlocationid'
     THEN regexp_replace(transformations.value ->> 'fieldId', 'permanentlocationid', 'permanentlocation.name', 'q')
	   WHEN transformations.value ->> 'fieldId' LIKE '%temporarylocationid'
     THEN regexp_replace(transformations.value ->> 'fieldId', 'temporarylocationid', 'temporarylocation.name', 'q')
	   WHEN transformations.value ->> 'fieldId' LIKE '%effectivelocationid'
     THEN regexp_replace(transformations.value ->> 'fieldId', 'effectivelocationid', 'effectivelocation.name', 'q')
	   WHEN transformations.value ->> 'recordType' = 'ITEM' AND transformations.value ->> 'fieldId' LIKE '%effectivecallnumbercomponents.callnumber%'
     THEN regexp_replace(transformations.value ->> 'fieldId', 'effectivecallnumbercomponents.callnumber', 'callnumber', 'q')
     ELSE transformations.value ->> 'fieldId'
    END
    )))
  FROM ${myuniversity}_${mymodule}.mapping_profiles, jsonb_array_elements(jsonb -> 'transformations') AS transformations
  WHERE ${myuniversity}_${mymodule}.mapping_profiles.id = profiles.id
  GROUP BY id
))
WHERE profiles.jsonb ->> 'transformations' IS NOT null AND jsonb_array_length(profiles.jsonb -> 'transformations') > 0;
