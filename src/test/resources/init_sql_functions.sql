CREATE FUNCTION public.unaccent(fieldValue text)
returns text
LANGUAGE plpgsql
AS
$$
BEGIN
   return fieldValue;
END;
$$;
