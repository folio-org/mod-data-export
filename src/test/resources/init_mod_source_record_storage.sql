CREATE SCHEMA diku_mod_source_record_storage;
CREATE SCHEMA diku_mod_entities_links;
CREATE TYPE diku_mod_source_record_storage.record_state AS ENUM ('ACTUAL', 'DRAFT', 'OLD', 'DELETED');
CREATE TYPE diku_mod_source_record_storage.record_type AS ENUM ('MARC_BIB', 'MARC_AUTHORITY', 'MARC_HOLDING', 'EDIFACT');
CREATE TABLE diku_mod_source_record_storage.records_lb (id UUID, external_id UUID, leader_record_status character(1),
  state diku_mod_source_record_storage.record_state, record_type diku_mod_source_record_storage.record_type, suppress_discovery boolean DEFAULT false,
  generation integer);
CREATE TABLE diku_mod_source_record_storage.marc_records_lb (id UUID, content JSONB);
CREATE TABLE diku_mod_entities_links.authority (id UUID);
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('17eed93e-f9e2-4cb2-a52b-e9155acfc119', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "17eed93e-f9e2-4cb2-a52b-e9155acfc119"
                                                               },
                                                               {
                                                                 "i": "4a090b0f-9da3-40f1-ab17-33d6a1e3abae"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792cam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('7777793e-f9e2-4cb2-a52b-e9155acfc119', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "7777793e-f9e2-4cb2-a52b-e9155acfc119"
                                                               },
                                                               {
                                                                 "i": "1640f777-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792cam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('8888893e-f9e2-4cb2-a52b-e9155acfc119', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "8888893e-f9e2-4cb2-a52b-e9155acfc119"
                                                               },
                                                               {
                                                                 "i": "2320f178-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792cam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('555d1aea-222d-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "555d1aea-222d-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "4444f178-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792cam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('66661aea-222d-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "66661aea-222d-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "6666f178-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792dam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('77771aea-222d-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "77771aea-222d-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "7777f178-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792cam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('88881aea-222d-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "88881aea-222d-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "8888f178-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792dam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('66661aea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "66661aea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "6666f178-1111-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792dam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('77771aea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "77771aea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "7777f178-1111-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792cam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('88881aea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "88881aea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "8888f178-1111-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792dam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('10001aea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "10001aea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "10001178-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792dam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('20002aea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "20002aea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "20002178-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792cam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('30003aea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "30003aea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "30003178-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792dam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('40004aea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "40004aea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "40004178-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792dam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('50005aea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "50005aea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "50005178-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792cam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('60006aea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "60006aea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "60006178-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792dam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('111111ea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "111111ea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "11111150-7c9b-48b0-86eb-178a494e25fe"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792nam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('222222ea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "222222ea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "22222250-7c9b-48b0-86eb-178a494e25fe"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792nam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('333333ea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "333333ea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "33333350-7c9b-48b0-86eb-178a494e25fe"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792nam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('444444ea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "444444ea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "44444477-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792dam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('555555ea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "555555ea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "55555577-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792nam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('666666ea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "666666ea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "66666677-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792nam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('777777ea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "777777ea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "77777777-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792dam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('888888ea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "888888ea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "88888877-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792nam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('912349ea-1111-4d1d-957d-0abcdd0e9acd', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "912349ea-1111-4d1d-957d-0abcdd0e9acd"
                                                               },
                                                               {
                                                                 "i": "91234977-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792dam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('15471150-7c9b-48b0-86eb-178a494e25fe', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "15471150-7c9b-48b0-86eb-178a494e25fe"
                                                               },
                                                               {
                                                                 "i": "15461150-7c9b-48b0-86eb-178a494e25fe"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792dam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('25472250-7c9b-48b0-86eb-178a494e25fe', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "25472250-7c9b-48b0-86eb-178a494e25fe"
                                                               },
                                                               {
                                                                 "i": "25462250-7c9b-48b0-86eb-178a494e25fe"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792nam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('35473350-7c9b-48b0-86eb-178a494e25fe', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "35473350-7c9b-48b0-86eb-178a494e25fe"
                                                               },
                                                               {
                                                                 "i": "35463350-7c9b-48b0-86eb-178a494e25fe"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792dam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('7171713e-f9e2-4cb2-a52b-e9155acfc119', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "7171713e-f9e2-4cb2-a52b-e9155acfc119"
                                                               },
                                                               {
                                                                 "i": "71717177-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792cam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('7272723e-f9e2-4cb2-a52b-e9155acfc119', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "7272723e-f9e2-4cb2-a52b-e9155acfc119"
                                                               },
                                                               {
                                                                 "i": "72727277-f243-4e4a-bf1c-9e1e62b3171d"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792cam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('28eed93e-f9e2-4cb2-a52b-e9155acfc119', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "28eed93e-f9e2-4cb2-a52b-e9155acfc119"
                                                               },
                                                               {
                                                                 "i": "28090b0f-9da3-40f1-ab17-33d6a1e3abae"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792dam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('34eed93e-f9e2-4cb2-a52b-e9155acfc119', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "34eed93e-f9e2-4cb2-a52b-e9155acfc119"
                                                               },
                                                               {
                                                                 "i": "34090b0f-9da3-40f1-ab17-33d6a1e3abae"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792nam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('45eed93e-f9e2-4cb2-a52b-e9155acfc119', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "45eed93e-f9e2-4cb2-a52b-e9155acfc119"
                                                               },
                                                               {
                                                                 "i": "45090b0f-9da3-40f1-ab17-33d6a1e3abae"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792dam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('111ed93e-f9e2-4cb2-a52b-e9155acfc119', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "111ed93e-f9e2-4cb2-a52b-e9155acfc119"
                                                               },
                                                               {
                                                                 "i": "11190b0f-9da3-40f1-ab17-33d6a1e3abae"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792cam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type)
	VALUES ('17eed93e-f9e2-4cb2-a52b-e9155acfc119', '4a090b0f-9da3-40f1-ab17-33d6a1e3abae', 'c', 'ACTUAL', 'MARC_AUTHORITY');
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type)
	VALUES ('28eed93e-f9e2-4cb2-a52b-e9155acfc119', '28090b0f-9da3-40f1-ab17-33d6a1e3abae', 'd', 'ACTUAL', 'MARC_AUTHORITY');
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type)
	VALUES ('34eed93e-f9e2-4cb2-a52b-e9155acfc119', '34090b0f-9da3-40f1-ab17-33d6a1e3abae', 'n', 'DELETED', 'MARC_AUTHORITY');
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type)
	VALUES ('45eed93e-f9e2-4cb2-a52b-e9155acfc119', '45090b0f-9da3-40f1-ab17-33d6a1e3abae', 'd', 'DELETED', 'MARC_AUTHORITY');
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type)
	VALUES ('111ed93e-f9e2-4cb2-a52b-e9155acfc119', '11190b0f-9da3-40f1-ab17-33d6a1e3abae', 'c', 'OLD', 'MARC_AUTHORITY');
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
	VALUES ('7171713e-f9e2-4cb2-a52b-e9155acfc119', '71717177-f243-4e4a-bf1c-9e1e62b3171d', 'c', 'ACTUAL', 'MARC_BIB', true);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
    VALUES ('7272723e-f9e2-4cb2-a52b-e9155acfc119', '72727277-f243-4e4a-bf1c-9e1e62b3171d', 'c', 'ACTUAL', 'MARC_BIB', false);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
	VALUES ('7777793e-f9e2-4cb2-a52b-e9155acfc119', '1640f777-f243-4e4a-bf1c-9e1e62b3171d', 'c', 'ACTUAL', 'MARC_BIB', true);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('8888893e-f9e2-4cb2-a52b-e9155acfc119', '2320f178-f243-4e4a-bf1c-9e1e62b3171d', 'c', 'ACTUAL', 'MARC_BIB', false);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('555d1aea-222d-4d1d-957d-0abcdd0e9acd', '4444f178-f243-4e4a-bf1c-9e1e62b3171d', 'c', 'ACTUAL', 'MARC_BIB', false);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('66661aea-222d-4d1d-957d-0abcdd0e9acd', '6666f178-f243-4e4a-bf1c-9e1e62b3171d', 'd', 'ACTUAL', 'MARC_BIB', false);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('77771aea-222d-4d1d-957d-0abcdd0e9acd', '7777f178-f243-4e4a-bf1c-9e1e62b3171d', 'c', 'DELETED', 'MARC_BIB', false);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('88881aea-222d-4d1d-957d-0abcdd0e9acd', '8888f178-f243-4e4a-bf1c-9e1e62b3171d', 'd', 'DELETED', 'MARC_BIB', false);

INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('66661aea-1111-4d1d-957d-0abcdd0e9acd', '6666f178-1111-4e4a-bf1c-9e1e62b3171d', 'd', 'ACTUAL', 'MARC_BIB', true);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('77771aea-1111-4d1d-957d-0abcdd0e9acd', '7777f178-1111-4e4a-bf1c-9e1e62b3171d', 'c', 'DELETED', 'MARC_BIB', true);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('88881aea-1111-4d1d-957d-0abcdd0e9acd', '8888f178-1111-4e4a-bf1c-9e1e62b3171d', 'd', 'DELETED', 'MARC_BIB', true);

INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('10001aea-1111-4d1d-957d-0abcdd0e9acd', '10001178-f243-4e4a-bf1c-9e1e62b3171d', 'd', 'ACTUAL', 'MARC_BIB', false);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('20002aea-1111-4d1d-957d-0abcdd0e9acd', '20002178-f243-4e4a-bf1c-9e1e62b3171d', 'c', 'DELETED', 'MARC_BIB', false);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('30003aea-1111-4d1d-957d-0abcdd0e9acd', '30003178-f243-4e4a-bf1c-9e1e62b3171d', 'd', 'DELETED', 'MARC_BIB', false);

INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('40004aea-1111-4d1d-957d-0abcdd0e9acd', '40004178-f243-4e4a-bf1c-9e1e62b3171d', 'd', 'ACTUAL', 'MARC_BIB', true);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('50005aea-1111-4d1d-957d-0abcdd0e9acd', '50005178-f243-4e4a-bf1c-9e1e62b3171d', 'c', 'DELETED', 'MARC_BIB', true);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('60006aea-1111-4d1d-957d-0abcdd0e9acd', '60006178-f243-4e4a-bf1c-9e1e62b3171d', 'd', 'DELETED', 'MARC_BIB', true);

INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('111111ea-1111-4d1d-957d-0abcdd0e9acd', '11111150-7c9b-48b0-86eb-178a494e25fe', 'n', 'ACTUAL', 'MARC_HOLDING', true);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('222222ea-1111-4d1d-957d-0abcdd0e9acd', '22222250-7c9b-48b0-86eb-178a494e25fe', 'n', 'ACTUAL', 'MARC_HOLDING', false);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('333333ea-1111-4d1d-957d-0abcdd0e9acd', '33333350-7c9b-48b0-86eb-178a494e25fe', 'n', 'ACTUAL', 'MARC_HOLDING', false);

INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('444444ea-1111-4d1d-957d-0abcdd0e9acd', '44444477-f243-4e4a-bf1c-9e1e62b3171d', 'n', 'ACTUAL', 'MARC_HOLDING', true);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('555555ea-1111-4d1d-957d-0abcdd0e9acd', '55555577-f243-4e4a-bf1c-9e1e62b3171d', 'n', 'ACTUAL', 'MARC_HOLDING', false);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('666666ea-1111-4d1d-957d-0abcdd0e9acd', '66666677-f243-4e4a-bf1c-9e1e62b3171d', 'n', 'ACTUAL', 'MARC_HOLDING', false);

INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('777777ea-1111-4d1d-957d-0abcdd0e9acd', '77777777-f243-4e4a-bf1c-9e1e62b3171d', 'd', 'ACTUAL', 'MARC_HOLDING', true);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('888888ea-1111-4d1d-957d-0abcdd0e9acd', '88888877-f243-4e4a-bf1c-9e1e62b3171d', 'n', 'DELETED', 'MARC_HOLDING', true);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('912349ea-1111-4d1d-957d-0abcdd0e9acd', '91234977-f243-4e4a-bf1c-9e1e62b3171d', 'd', 'DELETED', 'MARC_HOLDING', false);

INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('15471150-7c9b-48b0-86eb-178a494e25fe', '15461150-7c9b-48b0-86eb-178a494e25fe', 'd', 'ACTUAL', 'MARC_HOLDING', true);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('25472250-7c9b-48b0-86eb-178a494e25fe', '25462250-7c9b-48b0-86eb-178a494e25fe', 'n', 'DELETED', 'MARC_HOLDING', false);
INSERT INTO diku_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type, suppress_discovery)
  VALUES ('35473350-7c9b-48b0-86eb-178a494e25fe', '35463350-7c9b-48b0-86eb-178a494e25fe', 'd', 'DELETED', 'MARC_HOLDING', false);

INSERT INTO diku_mod_entities_links.authority (id)
  VALUES ('4a090b0f-9da3-40f1-ab17-33d6a1e3abae');

CREATE SCHEMA central_mod_source_record_storage;
CREATE TYPE central_mod_source_record_storage.record_state AS ENUM ('ACTUAL', 'DRAFT', 'OLD', 'DELETED');
CREATE TYPE central_mod_source_record_storage.record_type AS ENUM ('MARC_BIB', 'MARC_AUTHORITY', 'MARC_HOLDING', 'EDIFACT');
CREATE TABLE central_mod_source_record_storage.records_lb (id UUID, external_id UUID, leader_record_status character(1),
  state diku_mod_source_record_storage.record_state, record_type diku_mod_source_record_storage.record_type, suppress_discovery boolean DEFAULT false,
  generation integer);
CREATE TABLE central_mod_source_record_storage.marc_records_lb (id UUID, content JSONB);
INSERT INTO central_mod_source_record_storage.marc_records_lb (id, content)
	VALUES ('ed0ad74c-98f1-11ee-b9d1-0242ac120002', '{
                                                       "fields": [
                                                         {
                                                           "001": "in00000001098"
                                                         },
                                                         {
                                                           "008": "210701t20222022nyua   c      001 0 eng d"
                                                         },
                                                         {
                                                           "999": {
                                                             "ind1": "f",
                                                             "ind2": "f",
                                                             "subfields": [
                                                               {
                                                                 "s": "ed0ad74c-98f1-11ee-b9d1-0242ac120002"
                                                               },
                                                               {
                                                                 "i": "26be956e-98f2-11ee-b9d1-0242ac120002"
                                                               }
                                                             ]
                                                           }
                                                         }
                                                       ],
                                                       "leader": "03792cam a2200697 i 4500"
                                                     }'
                                                     );
INSERT INTO central_mod_source_record_storage.records_lb (id, external_id, leader_record_status, state, record_type)
	VALUES ('ed0ad74c-98f1-11ee-b9d1-0242ac120002', '26be956e-98f2-11ee-b9d1-0242ac120002', 'c', 'ACTUAL', 'MARC_AUTHORITY');
