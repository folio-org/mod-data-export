UPDATE ${myuniversity}_${mymodule}.error_logs as errorLogs
	SET jsonb = jsonb_set(jsonb, '{errorMessageCode}', '"error.messagePlaceholder"')
	WHERE errorLogs.jsonb ->> 'reason' IS NOT null;

UPDATE ${myuniversity}_${mymodule}.error_logs as errorLogs
	SET jsonb = jsonb_set(jsonb #- '{reason}', '{errorMessageValues}', jsonb_build_array(jsonb -> 'reason'))
	WHERE errorLogs.jsonb ->> 'reason' IS NOT null;
