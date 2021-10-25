UPDATE ${myuniversity}_${mymodule}.file_definitions
SET jsonb = jsonb_set(jsonb, '{idType}', '"instance"');
