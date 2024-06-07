package org.folio.dataexp.service.export.strategies;

import jakarta.persistence.EntityManager;
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
import org.folio.dataexp.exception.TransformationRuleException;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.InstanceCentralTenantRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.InstanceWithHridEntityRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.repository.MarcInstanceRecordRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.processor.RuleProcessor;
import org.folio.reader.EntityReader;
import org.folio.spring.FolioExecutionContext;
import org.folio.writer.RecordWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marc4j.MarcException;
import org.marc4j.marc.impl.DataFieldImpl;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.folio.dataexp.service.export.Constants.DEFAULT_INSTANCE_MAPPING_PROFILE_ID;
import static org.folio.dataexp.service.export.Constants.HOLDINGS_KEY;
import static org.folio.dataexp.service.export.Constants.HRID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_HRID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_KEY;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstancesExportStrategyTest {

  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private InstanceCentralTenantRepository instanceCentralTenantRepository;
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
  @Mock
  private EntityManager entityManager;
  @Mock
  private ErrorLogService errorLogService;
  @Spy
  private RuleHandler ruleHandler;
  @Mock
  private FolioExecutionContext folioExecutionContext;

  @Captor
  private ArgumentCaptor<MappingProfile> mappingProfileArgumentCaptor;

  @InjectMocks
  private InstancesExportStrategy instancesExportStrategy;

  @BeforeEach
  void setUp() {
    instancesExportStrategy.errorLogService = errorLogService;
    instancesExportStrategy.entityManager = entityManager;
  }

  @Test
  void getMarcRecordsTest() {
    var mappingProfile =  new MappingProfile();
    mappingProfile.setDefault(true);

    var marcRecord = MarcRecordEntity.builder().externalId(UUID.randomUUID()).build();
    var recordFromCentralTenant = MarcRecordEntity.builder().externalId(UUID.randomUUID()).build();
    var ids = Set.of(marcRecord.getExternalId(), recordFromCentralTenant.getExternalId());

    when(marcRecordEntityRepository.findByExternalIdInAndRecordTypeIsAndStateIs(anySet(), anyString(), anyString())).thenReturn(new ArrayList<>(List.of(marcRecord)));
    when(consortiaService.getCentralTenantId()).thenReturn("central");
    when(marcInstanceRecordRepository.findByExternalIdIn(eq("central"), anySet())).thenReturn(new ArrayList<>(List.of(recordFromCentralTenant)));

    var actualMarcRecords = instancesExportStrategy.getMarcRecords(new HashSet<>(ids), mappingProfile, new ExportRequest(), UUID.randomUUID());
    assertEquals(2, actualMarcRecords.size());

    mappingProfile.setDefault(false);
    mappingProfile.setRecordTypes(List.of(RecordTypes.SRS));

    actualMarcRecords = instancesExportStrategy.getMarcRecords(new HashSet<>(ids), mappingProfile, new ExportRequest(), UUID.randomUUID());
    assertEquals(2, actualMarcRecords.size());
  }

  @Test
  void getMarcRecordsIfMappingProfileNotDefaultAndRecordsTypeNotSrsTest() {
    var mappingProfile =  new MappingProfile();
    var marcRecord = MarcRecordEntity.builder().externalId(UUID.randomUUID()).build();
    var recordFromCentralTenant = MarcRecordEntity.builder().externalId(UUID.randomUUID()).build();
    var ids = Set.of(marcRecord.getExternalId(), recordFromCentralTenant.getExternalId());

    var actualMarcRecords = instancesExportStrategy.getMarcRecords(new HashSet<>(ids), mappingProfile, new ExportRequest(), UUID.randomUUID());
    assertEquals(0, actualMarcRecords.size());
  }

  @Test
  void getIdentifierMessageTest() {
    var instance = "{'id' : 'uuid', 'title' : 'title', 'hrid' : '123'}";
    var instanceRecordEntity = InstanceEntity.builder().jsonb(instance).id(UUID.randomUUID()).build();

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceRecordEntity));

    var opt = instancesExportStrategy.getIdentifiers(UUID.randomUUID());

    assertTrue(opt.isPresent());
    assertEquals("Instance with HRID : 123", opt.get().getIdentifierHridMessage());

    assertEquals("uuid", opt.get().getAssociatedJsonObject().getAsString("id"));
    assertEquals("title", opt.get().getAssociatedJsonObject().getAsString("title"));
    assertEquals("123", opt.get().getAssociatedJsonObject().getAsString("hrid"));
  }

  @Test
  void getIdentifierMessageIfInstanceDoesNotExistTest() {
    var instanceId  = UUID.fromString("b9d26945-9757-4855-ae6e-fd5d2f7d778e");

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of());

    var opt = instancesExportStrategy.getIdentifiers(instanceId);

    assertTrue(opt.isPresent());
    assertEquals("Instance with ID : b9d26945-9757-4855-ae6e-fd5d2f7d778e", opt.get().getIdentifierHridMessage());
  }

  @Test
  void getGeneratedMarcTest() throws TransformationRuleException {
    var transformation = new Transformations();
    var mappingProfile =  new MappingProfile();
    mappingProfile.setDefault(false);
    mappingProfile.setTransformations(List.of(transformation));
    mappingProfile.setRecordTypes(List.of(RecordTypes.SRS, RecordTypes.ITEM, RecordTypes.HOLDINGS));

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
    doNothing().when(instancesExportStrategy.entityManager).clear();
    instancesExportStrategy.getGeneratedMarc(new HashSet<>(), mappingProfile, new ExportRequest(), UUID.randomUUID(), new ExportStrategyStatistic(new ExportedMarcListener(null, 1000, null)));

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
  void getGeneratedMarcIfMarcExceptionTest() throws TransformationRuleException {
    var transformation = new Transformations();
    var mappingProfile =  new MappingProfile();
    mappingProfile.setDefault(false);
    mappingProfile.setTransformations(List.of(transformation));
    mappingProfile.setRecordTypes(List.of(RecordTypes.SRS, RecordTypes.ITEM, RecordTypes.HOLDINGS));

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
    doThrow(new MarcException()).when(ruleProcessor).process(isA(EntityReader.class), isA(RecordWriter.class), any(), anyList(), any());

    var generatedMarcResult = instancesExportStrategy.getGeneratedMarc(new HashSet<>(), mappingProfile, new ExportRequest(), UUID.randomUUID(), new ExportStrategyStatistic(new ExportedMarcListener(null, 1000, null)));

    verify(ruleFactory).getRules(mappingProfileArgumentCaptor.capture());
    verify(ruleProcessor).process(isA(EntityReader.class), isA(RecordWriter.class), any(), anyList(), any());
    verify(ruleHandler).preHandle(isA(JSONObject.class), anyList());
    verify(errorLogService).saveWithAffectedRecord(isA(JSONObject.class), isA(String.class), any(), isA(MarcException.class));

    var actualMappingProfile = mappingProfileArgumentCaptor.getValue();
    assertTrue(actualMappingProfile.getDefault());
    assertEquals(3, actualMappingProfile.getRecordTypes().size());
    assertTrue(actualMappingProfile.getRecordTypes().contains(RecordTypes.ITEM));
    assertTrue(actualMappingProfile.getRecordTypes().contains(RecordTypes.HOLDINGS));
    assertEquals(1, actualMappingProfile.getTransformations().size());

    assertEquals(1, generatedMarcResult.getFailedIds().size());
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

    var generatedMarcResult = new GeneratedMarcResult(UUID.randomUUID());

    when(holdingsRecordEntityRepository.findByInstanceIdIs(instanceId)).thenReturn(List.of(holdingRecordEntity));
    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceEntity));
    when(itemEntityRepository.findByHoldingsRecordIdIn(anySet())).thenReturn(List.of(itemEntity));
    doNothing().when(instancesExportStrategy.entityManager).clear();

    var instancesWithHoldingsAndItems = instancesExportStrategy.getInstancesWithHoldingsAndItems(new HashSet<>(Set.of(instanceId, notExistId)),
        generatedMarcResult, mappingProfile);

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
  void getHoldingsWithInstanceAndItemsIfCentralTenantExistTest() {
    var notExistId = UUID.fromString("0eaa0eef-0000-0c0e-af00-000000ebc576");
    var instanceFromCentralTenant =  "{'id' : '0eaa0eef-0000-0c0e-af00-000000ebc576', 'hrid' : 'instCentralHrid'}";
    var holding = "{'id' : '0eaa7eef-9633-4c7e-af09-796315ebc576'}";
    var holdingId = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    var instance = "{'id' : '1eaa1eef-1633-4c7e-af09-796315ebc576', 'hrid' : 'instHrid'}";
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var item = "{'barcode' : 'itemBarcode'}";
    var holdingRecordEntity = HoldingsRecordEntity.builder().jsonb(holding).id(holdingId).instanceId(instanceId).build();
    var instanceEntity = InstanceEntity.builder().jsonb(instance).id(instanceId).build();
    var instanceEntityFromCentralTenant = InstanceEntity.builder().jsonb(instanceFromCentralTenant).id(notExistId).build();
    var itemEntity = ItemEntity.builder().id(UUID.randomUUID()).holdingsRecordId(holdingId).jsonb(item).build();
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.INSTANCE, RecordTypes.HOLDINGS, RecordTypes.ITEM));

    var generatedMarcResult = new GeneratedMarcResult(UUID.randomUUID());

    when(holdingsRecordEntityRepository.findByInstanceIdIs(instanceId)).thenReturn(List.of(holdingRecordEntity));
    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceEntity));
    when(itemEntityRepository.findByHoldingsRecordIdIn(anySet())).thenReturn(List.of(itemEntity));
    when(consortiaService.getCentralTenantId()).thenReturn("central");
    when(instanceCentralTenantRepository.findInstancesByIdIn("central", Set.of(notExistId))).thenReturn(List.of(instanceEntityFromCentralTenant));

    var instancesWithHoldingsAndItems = instancesExportStrategy.getInstancesWithHoldingsAndItems(new HashSet<>(Set.of(instanceId, notExistId)), generatedMarcResult, mappingProfile);

    verify(holdingsRecordEntityRepository).findByInstanceIdIs(any());
    assertEquals(2, instancesWithHoldingsAndItems.size());

    var jsonObject = instancesWithHoldingsAndItems.get(0);
    var holdingJson = (JSONObject)((JSONArray)jsonObject.get(HOLDINGS_KEY)).get(0);
    assertEquals("instHrid", holdingJson.getAsString(INSTANCE_HRID_KEY));

    var itemJsonArray = (JSONArray)holdingJson.get(ITEMS_KEY);
    assertEquals(1, itemJsonArray.size());

    jsonObject = instancesWithHoldingsAndItems.get(1);
    var instanceJson = (JSONObject)jsonObject.get(INSTANCE_KEY);
    assertEquals("instCentralHrid", instanceJson.get(HRID_KEY));

    assertEquals(0, generatedMarcResult.getFailedIds().size());
    assertEquals(0, generatedMarcResult.getNotExistIds().size());
  }

  @Test
  void getHoldingsWithInstanceAndItemsIfErrorConvertingInstanceToJsonTest() {
    var jobExecutionId = UUID.randomUUID();
    var invalidInstanceJson = "{'id'  '1eaa1eef-1633-4c7e-af09-796315ebc576' 'hrid'  'instHrid'}";
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var instanceEntity = InstanceEntity.builder().jsonb(invalidInstanceJson).id(instanceId).build();
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.INSTANCE, RecordTypes.HOLDINGS, RecordTypes.ITEM));

    var generatedMarcResult = new GeneratedMarcResult(jobExecutionId);

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceEntity));
    doNothing().when(instancesExportStrategy.entityManager).clear();

    var instancesWithHoldingsAndItems = instancesExportStrategy.getInstancesWithHoldingsAndItems(new HashSet<>(Set.of(instanceId)),
      generatedMarcResult, mappingProfile);

    assertEquals(0, instancesWithHoldingsAndItems.size());
    assertEquals(1, generatedMarcResult.getFailedIds().size());

    verify(errorLogService).saveGeneralError("Error converting to json instance by id 1eaa1eef-1633-4c7e-af09-796315ebc576", jobExecutionId);
  }

  @Test
  void getAdditionalMarcFieldsByExternalIdTest() throws TransformationRuleException {
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

  @Test
  void saveConvertJsonRecordToMarcRecordErrorIfNotRecordLongErrorTest() {
    var jobExecutionId = UUID.randomUUID();
    var instance = "{'id' : '1eaa1eef-1633-4c7e-af09-796315ebc576', 'hrid' : 'instHrid', 'title' : 'title'}";
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var instanceEntity = InstanceEntity.builder().jsonb(instance).id(instanceId).build();
    var marcRecord = MarcRecordEntity.builder().externalId(instanceId).build();
    var errorMessage = "error message";

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceEntity));

    instancesExportStrategy.saveConvertJsonRecordToMarcRecordError(marcRecord, jobExecutionId, new IOException(errorMessage));

    var expectedErrorMessage = "Error converting json to marc for record 1eaa1eef-1633-4c7e-af09-796315ebc576";
    verify(errorLogService).saveGeneralError(expectedErrorMessage, jobExecutionId);
  }

  @Test
  void saveConvertJsonRecordToMarcRecordErrorIfErrorRecordTooLongTest() {
    var jobExecutionId = UUID.randomUUID();
    var instance = "{'id' : '1eaa1eef-1633-4c7e-af09-796315ebc576', 'hrid' : 'instHrid', 'title' : 'title'}";
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var instanceEntity = InstanceEntity.builder().jsonb(instance).id(instanceId).build();
    var marcRecord = MarcRecordEntity.builder().externalId(instanceId).build();
    var errorMessage = "Record is too long to be a valid MARC binary record, it's length would be 113937 which is more thatn 99999 bytes 2024";

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceEntity));

    instancesExportStrategy.saveConvertJsonRecordToMarcRecordError(marcRecord, jobExecutionId, new IOException(errorMessage));
    verify(errorLogService).saveWithAffectedRecord(isA(JSONObject.class), isA(String.class), isA(String.class), isA(UUID.class));
  }
}
