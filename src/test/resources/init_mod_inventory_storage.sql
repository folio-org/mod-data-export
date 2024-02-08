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

CREATE TABLE IF NOT EXISTS diku_mod_inventory_storage.holdings_records_source
(
    id uuid NOT NULL,
    jsonb jsonb NOT NULL
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

INSERT INTO diku_mod_inventory_storage.holdings_record (id, jsonb) VALUES ('0c45bb50-7c9b-48b0-86eb-178a494e25fe',
'{
   "id": "0c45bb50-7c9b-48b0-86eb-178a494e25fe",
   "hrid": "hold000000000002",
   "callNumber": "K1 .M44",
   "copyNumber": "1",
   "instanceId": "011e1aea-111d-4d1d-957d-0abcdd0e9acd"
 }
'
);

INSERT INTO diku_mod_inventory_storage.holdings_record (id, jsonb) VALUES ('1f45bb50-7c9b-48b0-86eb-178a494e25fe',
'{
   "id": "1f45bb50-7c9b-48b0-86eb-178a494e25fe",
   "hrid": "hold000000000003",
   "callNumber": "K1 .M44",
   "copyNumber": "1",
   "instanceId": "011e1aea-111d-4d1d-957d-0abcdd0e9acd"
 }
'
);
