CREATE SCHEMA diku_mod_inventory_storage;
CREATE TABLE diku_mod_inventory_storage.instance (id UUID, jsonb JSONB);
CREATE TABLE diku_mod_inventory_storage.holdings_record (id UUID, jsonb JSONB, instanceid UUID);
CREATE TABLE diku_mod_inventory_storage.item (id UUID, jsonb JSONB, holdingsrecordid UUID);
CREATE TABLE diku_mod_inventory_storage.authority (id UUID);
CREATE TABLE IF NOT EXISTS diku_mod_inventory_storage.audit_holdings_record
(
    id uuid NOT NULL,
    jsonb jsonb NOT NULL,
    CONSTRAINT audit_holdings_record_pkey PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS diku_mod_inventory_storage.audit_instance
(
    id uuid NOT NULL,
    jsonb jsonb NOT NULL,
    CONSTRAINT audit_instance_pkey PRIMARY KEY (id)
);

INSERT INTO diku_mod_inventory_storage.instance (id, jsonb) VALUES ('011e1aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "011e1aea-222d-4d1d-957d-0abcdd0e9acd",
   "hrid": "hrid001",
   "title": "Magazine - Q1",
   "source": "FOLIO"
 }
'
);

INSERT INTO diku_mod_inventory_storage.instance (id, jsonb) VALUES ('011e1aea-111d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "011e1aea-111d-4d1d-957d-0abcdd0e9acd",
   "hrid": "hrid002",
   "title": "Magazine - Q1",
   "source": "FOLIO"
 }
'
);
