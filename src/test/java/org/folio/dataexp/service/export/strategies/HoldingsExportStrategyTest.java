package org.folio.dataexp.service.export.strategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.service.export.Constants.HOLDINGS_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_HRID_KEY;
import static org.folio.dataexp.service.export.Constants.ITEMS_KEY;
import static org.folio.dataexp.util.ErrorCode.ERROR_HOLDINGS_NO_PERMISSION;
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

import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.SneakyThrows;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.folio.dataexp.client.ConsortiumSearchClient;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.Holdings;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.folio.dataexp.domain.entity.ItemEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.exception.TransformationRuleException;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.HoldingsRecordEntityTenantRepository;
import org.folio.dataexp.repository.InstanceCentralTenantRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.repository.MarcInstanceRecordRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.dataexp.service.validators.PermissionsValidator;
import org.folio.processor.RuleProcessor;
import org.folio.reader.EntityReader;
import org.folio.spring.FolioExecutionContext;
import org.folio.writer.RecordWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marc4j.MarcException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

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
  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private ConsortiumSearchClient consortiumSearchClient;
  @Mock
  private MarcInstanceRecordRepository marcInstanceRecordRepository;
  @Mock
  private HoldingsRecordEntityTenantRepository holdingsRecordEntityTenantRepository;
  @Mock
  private InstanceCentralTenantRepository instanceCentralTenantRepository;
  @Spy
  private RuleHandler ruleHandler;
  @Mock
  private PermissionsValidator permissionsValidator;
  @Mock
  private UserService userService;

  @InjectMocks
  private HoldingsExportStrategy holdingsExportStrategy;

  @BeforeEach
  void setUp() {
    holdingsExportStrategy.folioExecutionContext = folioExecutionContext;
    holdingsExportStrategy.entityManager = entityManager;
    holdingsExportStrategy.errorLogService = errorLogService;
    holdingsExportStrategy.setInstanceEntityRepository(instanceEntityRepository);
  }

  @Test
  void getMarcRecordsTestIfDefaultMappingProfileTest() {
    when(consortiaService.getCentralTenantId(any())).thenReturn("centralTenant");
    var mappingProfile =  new MappingProfile();
    mappingProfile.setDefault(true);
    holdingsExportStrategy.getMarcRecords(new HashSet<>(), mappingProfile, new ExportRequest(),
        UUID.randomUUID());
    verify(marcRecordEntityRepository).findByExternalIdInAndRecordTypeIsAndStateIn(anySet(),
        isA(String.class), isA(Set.class));
  }

  @Test
  void getMarcRecordsTestIfRecordTypesSrsTest() {
    var mappingProfile =  new MappingProfile();
    mappingProfile.setDefault(false);
    mappingProfile.setRecordTypes(List.of(RecordTypes.SRS));
    holdingsExportStrategy.getMarcRecords(new HashSet<>(), mappingProfile, new ExportRequest(),
        UUID.randomUUID());
    verify(marcRecordEntityRepository,
        times(0))
        .findByExternalIdInAndRecordTypeIsAndStateIn(anySet(), isA(String.class), isA(Set.class));
  }

  @Test
  void getIdentifierMessageTest() {
    var holding = "{'hrid' : '123'}";
    var holdingRecordEntity = HoldingsRecordEntity.builder().jsonb(holding).id(UUID.randomUUID())
        .build();

    when(holdingsRecordEntityRepository.findByIdIn(anySet()))
        .thenReturn(List.of(holdingRecordEntity));

    var opt = holdingsExportStrategy.getIdentifiers(UUID.randomUUID());

    assertTrue(opt.isPresent());
    assertEquals("123", opt.get().getIdentifierHridMessage());
  }

  @Test
  void getGeneratedMarcTest() throws TransformationRuleException {
    var holding = "{'id' : '0eaa7eef-9633-4c7e-af09-796315ebc576'}";
    var holdingRecordEntity = HoldingsRecordEntity.builder().jsonb(holding)
        .id(UUID.randomUUID()).build();

    when(holdingsRecordEntityRepository.findByIdIn(anySet()))
        .thenReturn(List.of(holdingRecordEntity));
    holdingsExportStrategy.getGeneratedMarc(new HashSet<>(), new MappingProfile(),
        new ExportRequest(), UUID.randomUUID(),
        new ExportStrategyStatistic(new ExportedRecordsListener(null, 1000, null)));

    verify(ruleFactory).getRules(isA(MappingProfile.class));
    verify(ruleProcessor).process(isA(EntityReader.class), isA(RecordWriter.class), any(),
        anyList(), any());
    verify(ruleHandler).preHandle(isA(JSONObject.class), anyList());
  }

  @Test
  void getGeneratedMarcIfMarcExceptionTest() throws TransformationRuleException {
    var holding = "{'id' : '0eaa7eef-9633-4c7e-af09-796315ebc576'}";
    var holdingRecordEntity = HoldingsRecordEntity.builder().jsonb(holding).id(UUID.randomUUID())
        .build();

    when(holdingsRecordEntityRepository.findByIdIn(anySet()))
        .thenReturn(List.of(holdingRecordEntity));
    doThrow(new MarcException("marc error")).when(ruleProcessor).process(isA(EntityReader.class),
        isA(RecordWriter.class), any(), anyList(), any());
    var generatedMarcResult = holdingsExportStrategy.getGeneratedMarc(new HashSet<>(),
        new MappingProfile(), new ExportRequest(), UUID.randomUUID(), new ExportStrategyStatistic(
            new ExportedRecordsListener(null, 1000, null)));
    assertEquals(1, generatedMarcResult.getFailedIds().size());
    var actualErrorMessage =
        List.of("marc error for holding 0eaa7eef-9633-4c7e-af09-796315ebc576");
    verify(ruleFactory).getRules(isA(MappingProfile.class));
    verify(ruleHandler).preHandle(isA(JSONObject.class), anyList());
    verify(errorLogService).saveGeneralErrorWithMessageValues(isA(String.class),
        eq(actualErrorMessage), isA(UUID.class));
  }

  @Test
  void getHoldingsWithInstanceAndItemsTest() {
    var holding = "{'id' : '0eaa7eef-9633-4c7e-af09-796315ebc576'}";
    var holdingId = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    var instance = "{'id' : '1eaa1eef-1633-4c7e-af09-796315ebc576', 'hrid' : 'instHrid'}";
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var item = "{'barcode' : 'itemBarcode'}";
    var holdingRecordEntity = HoldingsRecordEntity.builder().jsonb(holding).id(holdingId)
        .instanceId(instanceId).build();
    var instanceEntity = InstanceEntity.builder().jsonb(instance).id(instanceId).build();
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.ITEM));
    var itemEntity = ItemEntity.builder().id(UUID.randomUUID()).jsonb(item).build();

    var generatedMarcResult = new GeneratedMarcResult(UUID.randomUUID());

    when(holdingsRecordEntityRepository.findByIdIn(anySet()))
        .thenReturn(List.of(holdingRecordEntity));
    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceEntity));
    when(itemEntityRepository.findByHoldingsRecordIdIs(holdingId))
        .thenReturn(List.of(itemEntity));
    doNothing().when(holdingsExportStrategy.entityManager).clear();

    var holdingsWithInstanceAndItems = holdingsExportStrategy.getHoldingsWithInstanceAndItems(
        new HashSet<>(Set.of(holdingId)), generatedMarcResult, mappingProfile, UUID.randomUUID());

    assertEquals(1, holdingsWithInstanceAndItems.size());

    var jsonObject = holdingsWithInstanceAndItems.values().iterator().next();
    var holdingJson = (JSONObject) ((JSONArray) jsonObject.get(HOLDINGS_KEY)).get(0);
    assertEquals("instHrid", holdingJson.getAsString(INSTANCE_HRID_KEY));

    var itemJsonArray = (JSONArray) holdingJson.get(ITEMS_KEY);
    assertEquals(1, itemJsonArray.size());
  }

  @Test
  void getHoldingsWithInstanceAndItemsIfErrorConvertingHoldingToJsonTest() {
    var jobExecutionId = UUID.randomUUID();
    var invalidHoldingJson = "{'id'  '0eaa7eef-9633-4c7e-af09-796315ebc576'}";
    var holdingId = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    var instance = "{'id' : '1eaa1eef-1633-4c7e-af09-796315ebc576', 'hrid' : 'instHrid'}";
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var holdingRecordEntity = HoldingsRecordEntity.builder().jsonb(invalidHoldingJson)
        .id(holdingId).instanceId(instanceId).build();
    var instanceEntity = InstanceEntity.builder().jsonb(instance).id(instanceId).build();
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.ITEM));

    var generatedMarcResult = new GeneratedMarcResult(jobExecutionId);

    when(holdingsRecordEntityRepository.findByIdIn(anySet()))
        .thenReturn(List.of(holdingRecordEntity));
    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceEntity));
    doNothing().when(holdingsExportStrategy.entityManager).clear();

    var holdingsWithInstanceAndItems = holdingsExportStrategy.getHoldingsWithInstanceAndItems(
        new HashSet<>(Set.of(holdingId)), generatedMarcResult, mappingProfile, jobExecutionId);

    assertEquals(0, holdingsWithInstanceAndItems.size());
    assertEquals(1, generatedMarcResult.getFailedIds().size());

    verify(errorLogService).saveGeneralError(
        "Error converting to json holding by id 0eaa7eef-9633-4c7e-af09-796315ebc576",
        jobExecutionId);
  }

  @Test
  void getMarcRecordsTestIfCurrentTenantIsCentral() {
    var userId = UUID.randomUUID();
    when(consortiaService.getCentralTenantId("centralTenant"))
        .thenReturn("centralTenant");
    when(folioExecutionContext.getTenantId()).thenReturn("centralTenant");
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(consortiaService.getAffiliatedTenants("centralTenant", userId.toString()))
        .thenReturn(List.of("memberA", "memberB"));
    Holdings holdingsA = new Holdings();
    holdingsA.setTenantId("memberA");
    Holdings holdingsB = new Holdings();
    holdingsB.setTenantId("memberB");
    Holdings holdingsC = new Holdings();
    holdingsC.setTenantId("centralTenant");
    var uuidA = UUID.randomUUID();
    var uuidB = UUID.randomUUID();
    when(consortiumSearchClient.getHoldingsById(uuidA.toString())).thenReturn(holdingsA);
    when(consortiumSearchClient.getHoldingsById(uuidB.toString())).thenReturn(holdingsB);
    var uuidC = UUID.randomUUID();
    when(consortiumSearchClient.getHoldingsById(uuidC.toString())).thenReturn(holdingsC);
    when(marcInstanceRecordRepository.findByExternalIdIn("centralTenant", Set.of(uuidC)))
        .thenReturn(List.of(new MarcRecordEntity().withExternalId(uuidC)));
    when(marcInstanceRecordRepository.findByExternalIdIn("memberA", Set.of(uuidA)))
        .thenReturn(List.of(new MarcRecordEntity().withExternalId(uuidA)));
    when(marcInstanceRecordRepository.findByExternalIdIn("memberB", Set.of(uuidB)))
        .thenReturn(List.of(new MarcRecordEntity().withExternalId(uuidB)));
    when(permissionsValidator.checkInstanceViewPermissions(any(String.class))).thenReturn(true);
    var mappingProfile =  new MappingProfile();
    mappingProfile.setDefault(true);
    var ids = Set.of(uuidA, uuidB, uuidC);
    var res = holdingsExportStrategy.getMarcRecords(ids, mappingProfile, new ExportRequest(),
        UUID.randomUUID());
    assertThat(res).hasSize(3);
  }

  @Test
  void getFolioRecordsTestIfCurrentTenantIsCentral() {
    when(consortiaService.getCentralTenantId(any())).thenReturn("centralTenant");
    when(folioExecutionContext.getTenantId()).thenReturn("centralTenant");
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());
    when(consortiaService.getAffiliatedTenants(any(), any())).thenReturn(List.of("memberA",
        "memberB"));
    Holdings holdingsA = new Holdings();
    holdingsA.setTenantId("memberA");
    Holdings holdingsB = new Holdings();
    holdingsB.setTenantId("memberB");
    Holdings holdingsC = new Holdings();
    holdingsC.setTenantId("centralTenant");
    var uuidA = UUID.randomUUID();
    var uuidB = UUID.randomUUID();
    var uuidC = UUID.randomUUID();
    when(consortiumSearchClient.getHoldingsById(uuidA.toString())).thenReturn(holdingsA);
    when(consortiumSearchClient.getHoldingsById(uuidB.toString())).thenReturn(holdingsB);
    when(consortiumSearchClient.getHoldingsById(uuidC.toString())).thenReturn(holdingsC);
    when(permissionsValidator.checkInstanceViewPermissions(any(String.class))).thenReturn(true);
    var instId = UUID.randomUUID();
    when(instanceCentralTenantRepository.findInstancesByIdIn("centralTenant", Set.of(instId)))
        .thenReturn(
            List.of(new InstanceEntity().withId(instId).withJsonb("{\n"
                + "  \"id\": \"" + instId + "\",\n"
                + "  \"hrid\": \"in00000000024\"\n"
                + "}")));
    when(holdingsRecordEntityTenantRepository.findByIdIn("memberB", Set.of(uuidB)))
        .thenReturn(
            List.of(new HoldingsRecordEntity().withId(uuidC).withInstanceId(instId).withJsonb("{\n"
                + "  \"id\": \"" + uuidB + "\",\n"
                + "  \"hrid\": \"ho00000000009\",\n"
                + "  \"instanceId\": \"" + instId + "\"\n"
                + "}")));
    when(folioExecutionContext.getOkapiHeaders())
        .thenReturn(Map.of("x-okapi-token", List.of("token")));
    var ids = new HashSet<>(Set.of(uuidA, uuidB, uuidC));
    var mappingProfile =  new MappingProfile();
    mappingProfile.setDefault(true);
    var res = holdingsExportStrategy.getGeneratedMarc(ids, mappingProfile, new ExportRequest(),
        UUID.randomUUID(), new ExportStrategyStatistic(null));
    assertThat(res.getMarcRecords()).hasSize(1);
  }

  @Test
  @SneakyThrows
  void getHoldingsWithInstanceAndItems_whenNotEnoughPermissionsTest() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(List.of(RecordTypes.ITEM));
    var holdings = new Holdings();
    holdings.setTenantId("college");
    var jobExecutionId = UUID.randomUUID();

    var generatedMarcResult = new GeneratedMarcResult(jobExecutionId);

    when(folioExecutionContext.getTenantId()).thenReturn("central");
    when(consortiaService.getCentralTenantId("central")).thenReturn("central");
    var userId = UUID.randomUUID();
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    var holdingId = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    when(consortiumSearchClient.getHoldingsById(holdingId.toString())).thenReturn(holdings);
    when(consortiaService.getAffiliatedTenants("central", userId.toString()))
        .thenReturn(List.of("college"));
    when(permissionsValidator.checkInstanceViewPermissions("college"))
        .thenReturn(false);
    doNothing().when(holdingsExportStrategy.entityManager).clear();
    when(userService.getUserName("central", userId.toString()))
        .thenReturn("central_admin");

    var holdingsWithInstanceAndItems = holdingsExportStrategy.getHoldingsWithInstanceAndItems(
        new HashSet<>(Set.of(holdingId)), generatedMarcResult, mappingProfile, jobExecutionId);

    assertEquals(0, holdingsWithInstanceAndItems.size());
    var msgValues = List.of(holdingId.toString(), userService.getUserName(
        folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString()),
        "college");
    verify(errorLogService).saveGeneralErrorWithMessageValues(
        ERROR_HOLDINGS_NO_PERMISSION.getCode(), msgValues, jobExecutionId);
  }
}
