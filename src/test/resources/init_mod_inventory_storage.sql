CREATE SCHEMA diku_mod_inventory_storage;
CREATE TABLE diku_mod_inventory_storage.instance (id UUID, jsonb JSONB);
CREATE TABLE diku_mod_inventory_storage.holdings_record (id UUID);
CREATE TABLE diku_mod_inventory_storage.item (id UUID);
CREATE TABLE diku_mod_inventory_storage.authority (id UUID);

INSERT INTO diku_mod_inventory_storage.instance (id, jsonb) VALUES ('011e1aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "011e1aea-222d-4d1d-957d-0abcdd0e9acd",
   "hrid": "hrid001",
   "title": "Magazine - Q1",
   "source": "FOLIO"
 }
'
);
