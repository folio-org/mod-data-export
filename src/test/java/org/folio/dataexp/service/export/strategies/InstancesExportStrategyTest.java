package org.folio.dataexp.service.export.strategies;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.folio.dataexp.domain.entity.InstanceWithHridEntity;
import org.folio.dataexp.domain.entity.ItemEntity;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.InstanceWithHridEntityRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.repository.MarcInstanceRecordRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.processor.RuleProcessor;
import org.folio.reader.EntityReader;
import org.folio.writer.RecordWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marc4j.marc.impl.DataFieldImpl;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.folio.dataexp.service.export.Constants.DEFAULT_INSTANCE_MAPPING_PROFILE_ID;
import static org.folio.dataexp.service.export.Constants.HOLDINGS_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_HRID_KEY;
import static org.folio.dataexp.service.export.Constants.ITEMS_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstancesExportStrategyTest {

  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private MarcInstanceRecordRepository marcInstanceRecordRepository;
  @Mock
  private MarcRecordEntityRepository marcRecordEntityRepository;
  @Mock
  private InstanceEntityRepository instanceEntityRepository;
  @Mock
  private InstanceWithHridEntityRepository instanceWithHridEntityRepository;
  @Mock
  private RuleProcessor ruleProcessor;
  @Mock
  private RuleFactory ruleFactory;
  @Mock
  private ReferenceDataProvider referenceDataProvider;
  @Mock
  private HoldingsRecordEntityRepository holdingsRecordEntityRepository;
  @Mock
  private ItemEntityRepository itemEntityRepository;
  @Mock
  private MappingProfileEntityRepository mappingProfileEntityRepository;
  @Spy
  private RuleHandler ruleHandler;

  @Captor
  private ArgumentCaptor<MappingProfile> mappingProfileArgumentCaptor;

  @InjectMocks
  private InstancesExportStrategy instancesExportStrategy;

  @Test
  void getMarcRecordsTest() {
    var mappingProfile =  new MappingProfile();
    mappingProfile.setDefault(true);

    var marcRecord = MarcRecordEntity.builder().externalId(UUID.randomUUID()).build();
    var recordFromCentralTenant = MarcRecordEntity.builder().externalId(UUID.randomUUID()).build();
    var ids = Set.of(marcRecord.getExternalId(), recordFromCentralTenant.getExternalId());

    when(marcRecordEntityRepository.findByExternalIdInAndRecordTypeIs(anySet(), anyString())).thenReturn(new ArrayList<>(List.of(marcRecord)));
    when(consortiaService.getCentralTenantId()).thenReturn("central");
    when(marcInstanceRecordRepository.findByExternalIdIn(eq("central"), anySet())).thenReturn(new ArrayList<>(List.of(recordFromCentralTenant)));

    var actualMarcRecords = instancesExportStrategy.getMarcRecords(new HashSet<>(ids), mappingProfile, new ExportRequest());
    assertEquals(2, actualMarcRecords.size());

    mappingProfile.setDefault(false);
    mappingProfile.setRecordTypes(List.of(RecordTypes.SRS));

    actualMarcRecords = instancesExportStrategy.getMarcRecords(new HashSet<>(ids), mappingProfile, new ExportRequest());
    assertEquals(2, actualMarcRecords.size());
  }

  @Test
  void getMarcRecordsIfMappingProfileNotDefaultAndRecordsTypeNotSrsTest() {
    var mappingProfile =  new MappingProfile();
    var marcRecord = MarcRecordEntity.builder().externalId(UUID.randomUUID()).build();
    var recordFromCentralTenant = MarcRecordEntity.builder().externalId(UUID.randomUUID()).build();
    var ids = Set.of(marcRecord.getExternalId(), recordFromCentralTenant.getExternalId());

    var actualMarcRecords = instancesExportStrategy.getMarcRecords(new HashSet<>(ids), mappingProfile, new ExportRequest());
    assertEquals(0, actualMarcRecords.size());
  }

  @Test
  void getIdentifierMessageTest() {
    var instance = "{'hrid' : '123'}";
    var instanceRecordEntity = InstanceEntity.builder().jsonb(instance).id(UUID.randomUUID()).build();

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceRecordEntity));

    var opt = instancesExportStrategy.getIdentifierMessage(UUID.randomUUID());

    assertTrue(opt.isPresent());
    assertEquals("Instance with hrid : 123", opt.get());
  }

  @Test
  void getGeneratedMarcTest() {
    var transformation = new Transformations();
    var mappingProfile =  new MappingProfile();
    mappingProfile.setDefault(false);
    mappingProfile.setTransformations(List.of(transformation));
    mappingProfile.setRecordTypes(List.of(RecordTypes.ITEM, RecordTypes.HOLDINGS));

    var defaultMappingProfile =  new MappingProfile();
    defaultMappingProfile.setDefault(true);
    defaultMappingProfile.setRecordTypes(List.of(RecordTypes.INSTANCE));
    defaultMappingProfile.setId(UUID.fromString(DEFAULT_INSTANCE_MAPPING_PROFILE_ID));
    var defaultMappingProfileEntity = MappingProfileEntity.builder()
        .mappingProfile(defaultMappingProfile).id(defaultMappingProfile.getId()).build();

    var instance = "{'id' : '0eaa7eef-9633-4c7e-af09-796315ebc576'}";
    var instanceEntity = InstanceEntity.builder().jsonb(instance).id(UUID.randomUUID()).build();

    when(mappingProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(defaultMappingProfileEntity);
    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceEntity));
    when(mappingProfileEntityRepository.getReferenceById(defaultMappingProfile.getId())).thenReturn(defaultMappingProfileEntity);
    instancesExportStrategy.getGeneratedMarc(new HashSet<>(), mappingProfile, new ExportRequest(), true, true,
        UUID.randomUUID(), new ExportStrategyStatistic());

    verify(ruleFactory).getRules(mappingProfileArgumentCaptor.capture());

    verify(ruleProcessor).process(isA(EntityReader.class), isA(RecordWriter.class), any(), anyList(), any());
    verify(ruleHandler).preHandle(isA(JSONObject.class), anyList());

    var actualMappingProfile = mappingProfileArgumentCaptor.getValue();
    assertTrue(actualMappingProfile.getDefault());
    assertEquals(3, actualMappingProfile.getRecordTypes().size());
    assertTrue(actualMappingProfile.getRecordTypes().contains(RecordTypes.ITEM));
    assertTrue(actualMappingProfile.getRecordTypes().contains(RecordTypes.HOLDINGS));
    assertEquals(1, actualMappingProfile.getTransformations().size());
  }

  @Test
  void getHoldingsWithInstanceAndItemsTest() {
    var notExistId = UUID.fromString("0eaa0eef-0000-0c0e-af00-000000ebc576");
    var holding = "{'id' : '0eaa7eef-9633-4c7e-af09-796315ebc576'}";
    var holdingId = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    var instance = "{'id' : '1eaa1eef-1633-4c7e-af09-796315ebc576', 'hrid' : 'instHrid'}";
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var item = "{'barcode' : 'itemBarcode'}";
    var holdingRecordEntity = HoldingsRecordEntity.builder().jsonb(holding).id(holdingId).instanceId(instanceId).build();
    var instanceEntity = InstanceEntity.builder().jsonb(instance).id(instanceId).build();
    var itemEntity = ItemEntity.builder().id(UUID.randomUUID()).holdingsRecordId(holdingId).jsonb(item).build();
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.INSTANCE, RecordTypes.HOLDINGS, RecordTypes.ITEM));

    var generatedMarcResult = new GeneratedMarcResult();

    when(holdingsRecordEntityRepository.findByInstanceIdIs(instanceId)).thenReturn(List.of(holdingRecordEntity));
    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceEntity));
    when(itemEntityRepository.findByHoldingsRecordIdIn(anySet())).thenReturn(List.of(itemEntity));

    var instancesWithHoldingsAndItems = instancesExportStrategy.getInstancesWithHoldingsAndItems(new HashSet<>(Set.of(instanceId, notExistId)),
        generatedMarcResult, mappingProfile, new ExportRequest(), true, true);

    assertEquals(1, instancesWithHoldingsAndItems.size());

    var jsonObject = instancesWithHoldingsAndItems.get(0);
    var holdingJson = (JSONObject)((JSONArray)jsonObject.get(HOLDINGS_KEY)).get(0);
    assertEquals("instHrid", holdingJson.getAsString(INSTANCE_HRID_KEY));

    var itemJsonArray = (JSONArray)holdingJson.get(ITEMS_KEY);
    assertEquals(1, itemJsonArray.size());

    assertEquals(1, generatedMarcResult.getFailedIds().size());
    assertEquals(notExistId, generatedMarcResult.getFailedIds().get(0));
    assertEquals(1, generatedMarcResult.getNotExistIds().size());
    assertEquals(notExistId, generatedMarcResult.getFailedIds().get(0));
  }

  @Test
  void getAdditionalMarcFieldsByExternalIdTest() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.HOLDINGS, RecordTypes.ITEM));
    var instanceId = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    var marcRecord = MarcRecordEntity.builder().externalId(instanceId).build();
    var instanceHridEntity = InstanceWithHridEntity.builder().id(instanceId).hrid("instanceHrid").build();
    var holding = "{'id' : '0eaa7eef-9633-4c7e-af09-796315ebc576'}";
    var holdingId = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    var holdingRecordEntity = HoldingsRecordEntity.builder().jsonb(holding).id(holdingId).instanceId(instanceId).build();
    var item = "{'barcode' : 'itemBarcode'}";
    var itemEntity = ItemEntity.builder().id(UUID.randomUUID()).holdingsRecordId(holdingId).jsonb(item).build();
    var variableField = new DataFieldImpl("tag", 'a', 'b');

    when(instanceWithHridEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceHridEntity));
    when(holdingsRecordEntityRepository.findByInstanceIdIs(instanceId)).thenReturn(List.of(holdingRecordEntity));
    when(instanceWithHridEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceHridEntity));
    when(itemEntityRepository.findByHoldingsRecordIdIn(anySet())).thenReturn(List.of(itemEntity));
    when(ruleProcessor.processFields(any(), any(), any(), anyList(), any())).thenReturn(List.of(variableField));

    var marcFieldsByExternalId= instancesExportStrategy.getAdditionalMarcFieldsByExternalId(List.of(marcRecord), mappingProfile);
    assertNotNull(marcFieldsByExternalId);

    var actualMarcField = marcFieldsByExternalId.get(instanceId);

    assertEquals("tag ab", actualMarcField.getHoldingItemsFields().get(0).toString());
  }
}
