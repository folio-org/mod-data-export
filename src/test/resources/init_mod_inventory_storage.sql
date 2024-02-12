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

INSERT INTO diku_mod_inventory_storage.holdings_records_source (id, jsonb) VALUES ('036ee84a-6afd-4c3c-9ad3-4a12ab875f59',
'{
   "id": "036ee84a-6afd-4c3c-9ad3-4a12ab875f59",
   "name": "MARC"
 }
'
);

INSERT INTO diku_mod_inventory_storage.holdings_records_source (id, jsonb) VALUES ('f32d531e-df79-46b3-8932-cdd35f7a2264',
'{
   "id": "f32d531e-df79-46b3-8932-cdd35f7a2264",
   "name": "FOLIO"
 }
'
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

INSERT INTO diku_mod_inventory_storage.instance (id, jsonb) VALUES ('6666f178-f243-4e4a-bf1c-9e1e62b3171d',
'{
   "id": "6666f178-f243-4e4a-bf1c-9e1e62b3171d",
   "hrid": "hrid003",
   "title": "Magazine - Q1",
   "source": "MARC"
 }
'
);

INSERT INTO diku_mod_inventory_storage.instance (id, jsonb) VALUES ('7777f178-f243-4e4a-bf1c-9e1e62b3171d',
'{
   "id": "7777f178-f243-4e4a-bf1c-9e1e62b3171d",
   "hrid": "hrid004",
   "title": "Magazine - Q1",
   "source": "MARC"
 }
'
);

INSERT INTO diku_mod_inventory_storage.instance (id, jsonb) VALUES ('8888f178-f243-4e4a-bf1c-9e1e62b3171d',
'{
   "id": "8888f178-f243-4e4a-bf1c-9e1e62b3171d",
   "hrid": "hrid005",
   "title": "Magazine - Q1",
   "source": "MARC"
 }
'
);

INSERT INTO diku_mod_inventory_storage.instance (id, jsonb) VALUES ('6666f178-1111-4e4a-bf1c-9e1e62b3171d',
'{
   "id": "6666f178-1111-4e4a-bf1c-9e1e62b3171d",
   "hrid": "hrid006",
   "title": "Magazine - Q1",
   "source": "MARC",
   "discoverySuppress": true
 }
'
);

INSERT INTO diku_mod_inventory_storage.instance (id, jsonb) VALUES ('7777f178-1111-4e4a-bf1c-9e1e62b3171d',
'{
   "id": "7777f178-1111-4e4a-bf1c-9e1e62b3171d",
   "hrid": "hrid007",
   "title": "Magazine - Q1",
   "source": "MARC",
   "discoverySuppress": true
 }
'
);

INSERT INTO diku_mod_inventory_storage.instance (id, jsonb) VALUES ('8888f178-1111-4e4a-bf1c-9e1e62b3171d',
'{
   "id": "8888f178-1111-4e4a-bf1c-9e1e62b3171d",
   "hrid": "hrid007",
   "title": "Magazine - Q1",
   "source": "MARC",
   "discoverySuppress": true
 }
'
);

INSERT INTO diku_mod_inventory_storage.instance (id, jsonb) VALUES ('71717177-f243-4e4a-bf1c-9e1e62b3171d',
'{
   "id": "71717177-f243-4e4a-bf1c-9e1e62b3171d",
   "hrid": "hrid027",
   "title": "Magazine - Q1",
   "source": "MARC",
   "discoverySuppress": true
 }
'
);

INSERT INTO diku_mod_inventory_storage.instance (id, jsonb) VALUES ('72727277-f243-4e4a-bf1c-9e1e62b3171d',
'{
   "id": "72727277-f243-4e4a-bf1c-9e1e62b3171d",
   "hrid": "hrid037",
   "title": "Magazine - Q1",
   "source": "MARC",
   "discoverySuppress": false
 }
'
);

INSERT INTO diku_mod_inventory_storage.holdings_record (id, jsonb) VALUES ('0c45bb50-7c9b-48b0-86eb-178a494e25fe',
'{
   "id": "0c45bb50-7c9b-48b0-86eb-178a494e25fe",
   "hrid": "hold000000000002",
   "callNumber": "K1 .M44",
   "copyNumber": "1",
   "instanceId": "011e1aea-111d-4d1d-957d-0abcdd0e9acd",
   "sourceId": "f32d531e-df79-46b3-8932-cdd35f7a2264"
 }
'
);

INSERT INTO diku_mod_inventory_storage.holdings_record (id, jsonb) VALUES ('1f45bb50-7c9b-48b0-86eb-178a494e25fe',
'{
   "id": "1f45bb50-7c9b-48b0-86eb-178a494e25fe",
   "hrid": "hold000000000003",
   "callNumber": "K1 .M44",
   "copyNumber": "1",
   "instanceId": "011e1aea-111d-4d1d-957d-0abcdd0e9acd",
   "sourceId": "f32d531e-df79-46b3-8932-cdd35f7a2264",
   "discoverySuppress": false
 }
'
);

INSERT INTO diku_mod_inventory_storage.holdings_record (id, jsonb) VALUES ('1033bb50-7c9b-48b0-86eb-178a494e25fe',
'{
   "id": "1033bb50-7c9b-48b0-86eb-178a494e25fe",
   "hrid": "hold000000000004",
   "callNumber": "K1 .M44",
   "copyNumber": "1",
   "instanceId": "011e1aea-111d-4d1d-957d-0abcdd0e9acd",
   "sourceId": "f32d531e-df79-46b3-8932-cdd35f7a2264",
   "discoverySuppress": true
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_instance (id, jsonb) VALUES ('222e1aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "222e1aea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "1640f777-f243-4e4a-bf1c-9e1e62b3171d",
     "source": "MARC",
     "discoverySuppress": true
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_instance (id, jsonb) VALUES ('444e1aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "444e1aea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "2320f178-f243-4e4a-bf1c-9e1e62b3171d",
     "source": "MARC",
     "discoverySuppress": false
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_instance (id, jsonb) VALUES ('544e1aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "555d1aea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "4444f178-f243-4e4a-bf1c-9e1e62b3171d",
     "source": "MARC"
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_instance (id, jsonb) VALUES ('10001aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "10001aea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "10001178-f243-4e4a-bf1c-9e1e62b3171d",
     "source": "MARC"
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_instance (id, jsonb) VALUES ('20002aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "20002aea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "20002178-f243-4e4a-bf1c-9e1e62b3171d",
     "source": "MARC"
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_instance (id, jsonb) VALUES ('30003aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "30003aea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "30003178-f243-4e4a-bf1c-9e1e62b3171d",
     "source": "MARC"
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_instance (id, jsonb) VALUES ('40004aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "40004aea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "40004178-f243-4e4a-bf1c-9e1e62b3171d",
     "source": "MARC",
     "discoverySuppress": true
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_instance (id, jsonb) VALUES ('50005aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "50005aea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "50005178-f243-4e4a-bf1c-9e1e62b3171d",
     "source": "MARC",
     "discoverySuppress": true
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_instance (id, jsonb) VALUES ('60006aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "60006aea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "60006178-f243-4e4a-bf1c-9e1e62b3171d",
     "source": "MARC",
     "discoverySuppress": true
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_instance (id, jsonb) VALUES ('333e1aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "333e1aea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "1640f178-f243-4e4a-bf1c-9e1e62b3171d",
     "source": "FOLIO",
     "discoverySuppress": false
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_instance (id, jsonb) VALUES ('555e1aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "555e1aea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "1770f178-f243-4e4a-bf1c-9e1e62b3171d",
     "source": "FOLIO",
     "discoverySuppress": true
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_instance (id, jsonb) VALUES ('666e1aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "666e1aea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "1880f178-f243-4e4a-bf1c-9e1e62b3171d",
     "source": "FOLIO"
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_holdings_record (id, jsonb) VALUES ('10331aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "10331aea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "1033f777-f243-4e4a-bf1c-9e1e62b3171d",
     "sourceId": "f32d531e-df79-46b3-8932-cdd35f7a2264",
     "discoverySuppress": true
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_holdings_record (id, jsonb) VALUES ('10891aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "10891aea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "1089f777-f243-4e4a-bf1c-9e1e62b3171d",
     "sourceId": "f32d531e-df79-46b3-8932-cdd35f7a2264",
     "discoverySuppress": false
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_holdings_record (id, jsonb) VALUES ('17981aea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "17981aea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "1798f777-f243-4e4a-bf1c-9e1e62b3171d",
     "sourceId": "f32d531e-df79-46b3-8932-cdd35f7a2264"
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.holdings_record (id, jsonb) VALUES ('11111150-7c9b-48b0-86eb-178a494e25fe',
'{
   "id": "11111150-7c9b-48b0-86eb-178a494e25fe",
   "hrid": "hold000000000005",
   "callNumber": "K1 .M44",
   "copyNumber": "1",
   "instanceId": "011e1aea-111d-4d1d-957d-0abcdd0e9acd",
   "sourceId": "036ee84a-6afd-4c3c-9ad3-4a12ab875f59",
   "discoverySuppress": true
 }
'
);

INSERT INTO diku_mod_inventory_storage.holdings_record (id, jsonb) VALUES ('22222250-7c9b-48b0-86eb-178a494e25fe',
'{
   "id": "22222250-7c9b-48b0-86eb-178a494e25fe",
   "hrid": "hold000000000006",
   "callNumber": "K1 .M44",
   "copyNumber": "1",
   "instanceId": "011e1aea-111d-4d1d-957d-0abcdd0e9acd",
   "sourceId": "036ee84a-6afd-4c3c-9ad3-4a12ab875f59",
   "discoverySuppress": false
 }
'
);

INSERT INTO diku_mod_inventory_storage.holdings_record (id, jsonb) VALUES ('33333350-7c9b-48b0-86eb-178a494e25fe',
'{
   "id": "33333350-7c9b-48b0-86eb-178a494e25fe",
   "hrid": "hold000000000007",
   "callNumber": "K1 .M44",
   "copyNumber": "1",
   "instanceId": "011e1aea-111d-4d1d-957d-0abcdd0e9acd",
   "sourceId": "036ee84a-6afd-4c3c-9ad3-4a12ab875f59"
 }
'
);

INSERT INTO diku_mod_inventory_storage.holdings_record (id, jsonb) VALUES ('15461150-7c9b-48b0-86eb-178a494e25fe',
'{
   "id": "15461150-7c9b-48b0-86eb-178a494e25fe",
   "hrid": "hold000000000026",
   "callNumber": "K1 .M44",
   "copyNumber": "1",
   "instanceId": "011e1aea-111d-4d1d-957d-0abcdd0e9acd",
   "sourceId": "036ee84a-6afd-4c3c-9ad3-4a12ab875f59",
   "discoverySuppress": true
 }
'
);

INSERT INTO diku_mod_inventory_storage.holdings_record (id, jsonb) VALUES ('25462250-7c9b-48b0-86eb-178a494e25fe',
'{
   "id": "25462250-7c9b-48b0-86eb-178a494e25fe",
   "hrid": "hold000000000016",
   "callNumber": "K1 .M44",
   "copyNumber": "1",
   "instanceId": "011e1aea-111d-4d1d-957d-0abcdd0e9acd",
   "sourceId": "036ee84a-6afd-4c3c-9ad3-4a12ab875f59",
   "discoverySuppress": false
 }
'
);

INSERT INTO diku_mod_inventory_storage.holdings_record (id, jsonb) VALUES ('35463350-7c9b-48b0-86eb-178a494e25fe',
'{
   "id": "35463350-7c9b-48b0-86eb-178a494e25fe",
   "hrid": "hold000000000017",
   "callNumber": "K1 .M44",
   "copyNumber": "1",
   "instanceId": "011e1aea-111d-4d1d-957d-0abcdd0e9acd",
   "sourceId": "036ee84a-6afd-4c3c-9ad3-4a12ab875f59"
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_holdings_record (id, jsonb) VALUES ('444444ea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "444444ea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "44444477-f243-4e4a-bf1c-9e1e62b3171d",
     "sourceId": "036ee84a-6afd-4c3c-9ad3-4a12ab875f59",
     "discoverySuppress": true
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_holdings_record (id, jsonb) VALUES ('555555ea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "555555ea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "55555577-f243-4e4a-bf1c-9e1e62b3171d",
     "sourceId": "036ee84a-6afd-4c3c-9ad3-4a12ab875f59",
     "discoverySuppress": false
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_holdings_record (id, jsonb) VALUES ('65566677-f243-4e4a-bf1c-9e1e62b3171d',
'{
   "id": "65566677-f243-4e4a-bf1c-9e1e62b3171d",
   "record": {
     "id": "66666677-f243-4e4a-bf1c-9e1e62b3171d",
     "sourceId": "036ee84a-6afd-4c3c-9ad3-4a12ab875f59",
     "discoverySuppress": false
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_holdings_record (id, jsonb) VALUES ('777777ea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "777777ea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "77777777-f243-4e4a-bf1c-9e1e62b3171d",
     "sourceId": "036ee84a-6afd-4c3c-9ad3-4a12ab875f59",
     "discoverySuppress": true
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_holdings_record (id, jsonb) VALUES ('888888ea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "888888ea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "88888877-f243-4e4a-bf1c-9e1e62b3171d",
     "sourceId": "036ee84a-6afd-4c3c-9ad3-4a12ab875f59",
     "discoverySuppress": true
   }
 }
'
);

INSERT INTO diku_mod_inventory_storage.audit_holdings_record (id, jsonb) VALUES ('999999ea-222d-4d1d-957d-0abcdd0e9acd',
'{
   "id": "999999ea-222d-4d1d-957d-0abcdd0e9acd",
   "record": {
     "id": "91234977-f243-4e4a-bf1c-9e1e62b3171d",
     "sourceId": "036ee84a-6afd-4c3c-9ad3-4a12ab875f59",
     "discoverySuppress": false
   }
 }
'
);