UPDATE ${myuniversity}_${mymodule}.mapping_profiles AS profiles
SET jsonb = jsonb_set(jsonb, '{transformations}', (
      SELECT jsonb_agg(transformations.value - 'hasSameTagInItems')
      FROM ${myuniversity}_${mymodule}.mapping_profiles, jsonb_array_elements(jsonb -> 'transformations') AS transformations
	    WHERE ${myuniversity}_${mymodule}.mapping_profiles.id = profiles.id
      GROUP BY jsonb
    ))
WHERE profiles.jsonb ->> 'transformations' IS NOT null AND jsonb_array_length(profiles.jsonb -> 'transformations') > 0;
