package org.folio.dataexp.service.export.strategies;

import jakarta.persistence.EntityManager;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.folio.dataexp.domain.entity.ItemEntity;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.processor.RuleProcessor;
import org.folio.reader.EntityReader;
import org.folio.writer.RecordWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marc4j.MarcException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.folio.dataexp.service.export.Constants.HOLDINGS_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_HRID_KEY;
import static org.folio.dataexp.service.export.Constants.ITEMS_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoldingsExportStrategyTest {

  @Mock
  private MarcRecordEntityRepository marcRecordEntityRepository;
  @Mock
  private HoldingsRecordEntityRepository holdingsRecordEntityRepository;
  @Mock
  private InstanceEntityRepository instanceEntityRepository;
  @Mock
  private RuleProcessor ruleProcessor;
  @Mock
  private ItemEntityRepository itemEntityRepository;
  @Mock
  private RuleFactory ruleFactory;
  @Mock
  private EntityManager entityManager;
  @Mock
  private ReferenceDataProvider referenceDataProvider;
  @Mock
  private ErrorLogService errorLogService;
  @Spy
  private RuleHandler ruleHandler;

  @InjectMocks
  private HoldingsExportStrategy holdingsExportStrategy;

  @BeforeEach
  void setUp() {
    holdingsExportStrategy.entityManager = entityManager;
  }

  @Test
  void getMarcRecordsTestIfDefaultMappingProfileTest() {
    var mappingProfile =  new MappingProfile();
    mappingProfile.setDefault(true);
    holdingsExportStrategy.getMarcRecords(new HashSet<>(), mappingProfile, new ExportRequest());
    verify(marcRecordEntityRepository).findByExternalIdInAndRecordTypeIsAndStateIs(anySet(), isA(String.class), isA(String.class));
  }

  @Test
  void getMarcRecordsTestIfRecordTypesSrsTest() {
    var mappingProfile =  new MappingProfile();
    mappingProfile.setDefault(false);
    mappingProfile.setRecordTypes(List.of(RecordTypes.SRS));
    holdingsExportStrategy.getMarcRecords(new HashSet<>(), mappingProfile, new ExportRequest());
    verify(marcRecordEntityRepository, times(0)).findByExternalIdInAndRecordTypeIsAndStateIs(anySet(), isA(String.class), isA(String.class));
  }

  @Test
  void getIdentifierMessageTest() {
    var holding = "{'hrid' : '123'}";
    var holdingRecordEntity = HoldingsRecordEntity.builder().jsonb(holding).id(UUID.randomUUID()).build();

    when(holdingsRecordEntityRepository.findByIdIn(anySet())).thenReturn(List.of(holdingRecordEntity));

    var opt = holdingsExportStrategy.getIdentifiers(UUID.randomUUID());

    assertTrue(opt.isPresent());
    assertEquals("Holding with hrid : 123", opt.get().getIdentifierHridMessage());
  }

  @Test
  void getGeneratedMarcTest() {
    var holding = "{'id' : '0eaa7eef-9633-4c7e-af09-796315ebc576'}";
    var holdingRecordEntity = HoldingsRecordEntity.builder().jsonb(holding).id(UUID.randomUUID()).build();

    when(holdingsRecordEntityRepository.findByIdIn(anySet())).thenReturn(List.of(holdingRecordEntity));
    holdingsExportStrategy.getGeneratedMarc(new HashSet<>(), new MappingProfile(), new ExportRequest(), UUID.randomUUID(), new ExportStrategyStatistic(new ExportedMarcListener()));

    verify(ruleFactory).getRules(isA(MappingProfile.class));
    verify(ruleProcessor).process(isA(EntityReader.class), isA(RecordWriter.class), any(), anyList(), any());
    verify(ruleHandler).preHandle(isA(JSONObject.class), anyList());
  }

  @Test
  void getGeneratedMarcIfMarcExceptionTest() {
    var holding = "{'id' : '0eaa7eef-9633-4c7e-af09-796315ebc576'}";
    var holdingRecordEntity = HoldingsRecordEntity.builder().jsonb(holding).id(UUID.randomUUID()).build();

    when(holdingsRecordEntityRepository.findByIdIn(anySet())).thenReturn(List.of(holdingRecordEntity));
    doThrow(new MarcException("marc error")).when(ruleProcessor).process(isA(EntityReader.class), isA(RecordWriter.class), any(), anyList(), any());
    var generatedMarcResult = holdingsExportStrategy.getGeneratedMarc(new HashSet<>(), new MappingProfile(), new ExportRequest(), UUID.randomUUID(), new ExportStrategyStatistic(new ExportedMarcListener()));

    var actualErrorMessage = List.of("marc error for holding 0eaa7eef-9633-4c7e-af09-796315ebc576");
    verify(ruleFactory).getRules(isA(MappingProfile.class));
    verify(ruleHandler).preHandle(isA(JSONObject.class), anyList());
    verify(errorLogService).saveGeneralErrorWithMessageValues( isA(String.class), eq(actualErrorMessage), isA(UUID.class));

    assertEquals(1, generatedMarcResult.getFailedIds().size());
  }

  @Test
  void getHoldingsWithInstanceAndItemsTest() {
    var holding = "{'id' : '0eaa7eef-9633-4c7e-af09-796315ebc576'}";
    var holdingId = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    var instance = "{'id' : '1eaa1eef-1633-4c7e-af09-796315ebc576', 'hrid' : 'instHrid'}";
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var item = "{'barcode' : 'itemBarcode'}";
    var holdingRecordEntity = HoldingsRecordEntity.builder().jsonb(holding).id(holdingId).instanceId(instanceId).build();
    var instanceEntity = InstanceEntity.builder().jsonb(instance).id(instanceId).build();
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.ITEM));
    var itemEntity = ItemEntity.builder().id(UUID.randomUUID()).jsonb(item).build();

    var generatedMarcResult = new GeneratedMarcResult();

    when(holdingsRecordEntityRepository.findByIdIn(anySet())).thenReturn(List.of(holdingRecordEntity));
    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceEntity));
    when(itemEntityRepository.findByHoldingsRecordIdIs(holdingId)).thenReturn(List.of(itemEntity));
    doNothing().when(holdingsExportStrategy.entityManager).clear();

    var holdingsWithInstanceAndItems = holdingsExportStrategy.getHoldingsWithInstanceAndItems(new HashSet<>(Set.of(holdingId)), generatedMarcResult, mappingProfile);

    assertEquals(1, holdingsWithInstanceAndItems.size());

    var jsonObject = holdingsWithInstanceAndItems.get(0);
    var holdingJson = (JSONObject)((JSONArray)jsonObject.get(HOLDINGS_KEY)).get(0);
    assertEquals("instHrid", holdingJson.getAsString(INSTANCE_HRID_KEY));

    var itemJsonArray = (JSONArray)holdingJson.get(ITEMS_KEY);
    assertEquals(1, itemJsonArray.size());
  }

}
