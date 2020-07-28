CREATE SEQUENCE IF NOT EXISTS ${myuniversity}_${mymodule}.job_execution_hrId MINVALUE 1 NO MAXVALUE CACHE 1 NO CYCLE;
ALTER SEQUENCE ${myuniversity}_${mymodule}.job_execution_hrId OWNER TO ${myuniversity}_${mymodule};
