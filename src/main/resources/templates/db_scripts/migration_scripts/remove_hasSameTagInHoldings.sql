UPDATE ${myuniversity}_${mymodule}.mapping_profiles
SET jsonb = jsonb_set(jsonb, '{transformations}', subselect.clean_transformations)
FROM
   (
      SELECT jsonb_agg(transformations.value - 'hasSameTagInHoldings') AS clean_transformations
      FROM ${myuniversity}_${mymodule}.mapping_profiles, jsonb_array_elements(jsonb -> 'transformations') AS transformations
      GROUP BY jsonb
    ) AS subselect;
