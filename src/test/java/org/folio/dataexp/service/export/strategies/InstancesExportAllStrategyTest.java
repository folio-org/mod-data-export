package org.folio.dataexp.service.export.strategies;

import static java.lang.String.format;
import static org.folio.dataexp.service.export.Constants.DELETED_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_KEY;
import static org.folio.dataexp.util.ErrorCode.ERROR_DELETED_TOO_LONG_INSTANCE;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minidev.json.JSONObject;
import org.apache.maven.shared.utils.StringUtils;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.AuditInstanceEntity;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.AuditInstanceEntityRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.dataexp.service.export.LocalStorageWriter;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.dataexp.util.ErrorCode;
import org.folio.processor.RuleProcessor;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.ap.internal.util.Collections;
import org.marc4j.MarcException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InstancesExportAllStrategyTest {

  @Mock
  private AuditInstanceEntityRepository auditInstanceEntityRepository;
  @Mock
  private InstanceEntityRepository instanceEntityRepository;
  @Mock
  private JobExecutionEntityRepository jobExecutionEntityRepository;
  @Mock
  private ErrorLogService errorLogService;
  @Mock
  private LocalStorageWriter localStorageWriter;
  @Mock
  private HoldingsItemsResolverService holdingsItemsResolver;
  @Mock
  private JsonToMarcConverter jsonToMarcConverter;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private ConsortiaService consortiaService;
  @Mock
  private ReferenceDataProvider referenceDataProvider;
  @Mock
  private RuleFactory ruleFactory;
  @Mock
  private RuleHandler ruleHandler;
  @Mock
  private RuleProcessor ruleProcessor;

  @InjectMocks
  private InstancesExportAllStrategy instancesExportAllStrategy;

  @BeforeEach
  void setUp() {
    instancesExportAllStrategy.folioExecutionContext = folioExecutionContext;
  }

  @Test
  void getIdentifierMessageTest() {
    var auditInstanceEntity = AuditInstanceEntity.builder()
        .id(UUID.randomUUID()).hrid("123").title("title").build();

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of());
    when(auditInstanceEntityRepository.findByIdIn(anySet()))
        .thenReturn(List.of(auditInstanceEntity));

    var opt = instancesExportAllStrategy.getIdentifiers(UUID.randomUUID());

    assertTrue(opt.isPresent());
    assertEquals("Instance with HRID : 123", opt.get().getIdentifierHridMessage());

    assertEquals(auditInstanceEntity.getId().toString(), opt.get().getAssociatedJsonObject()
        .getAsString("id"));
    assertEquals("title", opt.get().getAssociatedJsonObject().getAsString("title"));
    assertEquals("123", opt.get().getAssociatedJsonObject().getAsString("hrid"));
  }

  @Test
  void getIdentifierMessageIfInstanceDoesNotExistTest() {
    var instanceId  = UUID.fromString("b9d26945-9757-4855-ae6e-fd5d2f7d778e");
    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of());
    when(auditInstanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of());

    var opt = instancesExportAllStrategy.getIdentifiers(instanceId);

    assertTrue(opt.isPresent());
    assertEquals("Instance with ID : b9d26945-9757-4855-ae6e-fd5d2f7d778e",
        opt.get().getIdentifierHridMessage());
  }

  @Test
  void saveConvertJsonRecordToMarcRecordErrorIfErrorRecordTooLongAndInstanceDeletedTest() {
    instancesExportAllStrategy.setErrorLogService(errorLogService);

    var auditInstanceEntity = AuditInstanceEntity.builder()
        .id(UUID.randomUUID()).hrid("123").title("title").build();
    var jobExecutionId = UUID.randomUUID();
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var marcRecord = MarcRecordEntity.builder().externalId(instanceId).id(UUID.randomUUID())
        .build();
    var errorMessage = "Record is too long to be a valid MARC binary record, it's length would "
        + "be 113937 which is more thatn 99999 bytes 2024";

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of());
    when(auditInstanceEntityRepository.findByIdIn(anySet()))
        .thenReturn(List.of(auditInstanceEntity));

    instancesExportAllStrategy.saveConvertJsonRecordToMarcRecordError(marcRecord,
        jobExecutionId, new IOException(errorMessage));
    verify(errorLogService).saveWithAffectedRecord(isA(JSONObject.class),
        eq(errorMessage), eq(ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode()),
        isA(UUID.class));
  }

  @Test
  void saveDuplicateErrorsIfInstanceDeletedTest() {
    instancesExportAllStrategy.setErrorLogService(errorLogService);
    instancesExportAllStrategy.setInstanceEntityRepository(instanceEntityRepository);

    ReflectionTestUtils.setField(instancesExportAllStrategy, "jsonToMarcConverter",
        jsonToMarcConverter);

    var auditInstanceEntity = AuditInstanceEntity.builder()
        .id(UUID.randomUUID()).hrid("123").title("title").build();
    var jobExecutionId = UUID.randomUUID();
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of());
    when(jobExecutionEntityRepository.getReferenceById(jobExecutionId)).thenReturn(
        new JobExecutionEntity().withJobExecution(new JobExecution().progress(
            new JobExecutionProgress())));
    when(auditInstanceEntityRepository.findByIdIn(anySet()))
        .thenReturn(List.of(auditInstanceEntity));

    var externalIds = Collections.asSet(instanceId);
    var statistic = new ExportStrategyStatistic(
        new ExportedMarcListener(jobExecutionEntityRepository, 1, jobExecutionId));
    var marcRecordDuplicate = MarcRecordEntity.builder().externalId(instanceId)
        .id(UUID.randomUUID()).build();
    var marcRecord = MarcRecordEntity.builder().externalId(instanceId).id(UUID.randomUUID())
        .deleted(true).build();
    instancesExportAllStrategy.createAndSaveMarcFromJsonRecord(externalIds, statistic,
        new MappingProfile(), jobExecutionId, Set.of(instanceId),
        List.of(marcRecord, marcRecordDuplicate), localStorageWriter);
    var expectedErrorMessage =
        format("Instance with HRID : 123 has following SRS records associated: %s, %s",
            marcRecord.getId(), marcRecordDuplicate.getId());
    verify(errorLogService).saveWithAffectedRecord(any(), eq(expectedErrorMessage),
        eq(ErrorCode.ERROR_DUPLICATE_SRS_RECORD.getCode()), eq(jobExecutionId));
  }

  @Test
  void saveInstanceTooLongErrorsIfInstanceDeletedTest() {
    instancesExportAllStrategy.setErrorLogService(errorLogService);

    ReflectionTestUtils.setField(instancesExportAllStrategy, "jsonToMarcConverter",
        jsonToMarcConverter);
    var auditInstanceEntity = AuditInstanceEntity.builder()
        .id(UUID.randomUUID()).hrid("123").title(generateTooLongString()).build();

    var instanceWithHoldingsAndItems = new JSONObject();
    var jsonInstance = new JSONObject();
    instanceWithHoldingsAndItems.put(INSTANCE_KEY, jsonInstance);
    jsonInstance.put("id", auditInstanceEntity.getId());
    jsonInstance.put("title", auditInstanceEntity.getTitle());
    jsonInstance.put(DELETED_KEY, true);
    var instancesWithHoldingsAndItems = List.of(instanceWithHoldingsAndItems);

    when(ruleProcessor.process(any(), any(), any(), any(), any())).thenThrow(new MarcException());

    var jobExecutionId = UUID.randomUUID();
    instancesExportAllStrategy.getGeneratedMarc(new GeneratedMarcResult(jobExecutionId),
        instancesWithHoldingsAndItems, new MappingProfile(), jobExecutionId);
    verify(errorLogService).saveWithAffectedRecord(isA(JSONObject.class),
        eq(ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode()), isA(UUID.class),
        isA(MarcException.class));
    verify(errorLogService).saveGeneralErrorWithMessageValues(
        eq(ERROR_DELETED_TOO_LONG_INSTANCE.getCode()),
        eq(List.of(auditInstanceEntity.getId().toString())),
        isA(UUID.class));
  }

  @Test
  void saveConvertJsonRecordToMarcRecordErrorIfErrorRecordTooLongAndInstanceNotDeletedTest() {
    instancesExportAllStrategy.setErrorLogService(errorLogService);

    var jobExecutionId = UUID.randomUUID();
    var instance =
        "{'id' : '1eaa1eef-1633-4c7e-af09-796315ebc576', 'hrid' : 'instHrid', 'title' : 'title'}";
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var instanceEntity = InstanceEntity.builder().jsonb(instance).id(instanceId).build();
    var marcRecord = MarcRecordEntity.builder().externalId(instanceId).build();
    var errorMessage =
        "Record is too long to be a valid MARC binary record, it's length would be 113937 which"
        + " is more than 99999 bytes 2024";

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceEntity));

    instancesExportAllStrategy.saveConvertJsonRecordToMarcRecordError(marcRecord, jobExecutionId,
        new IOException(errorMessage));

    verify(errorLogService).saveWithAffectedRecord(isA(JSONObject.class), eq(errorMessage),
        eq(ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode()), isA(UUID.class));
  }


  @Test
  void saveConvertJsonRecordToMarcRecordErrorIfNotErrorRecordTooLongTest() {
    instancesExportAllStrategy.setErrorLogService(errorLogService);

    var jobExecutionId = UUID.randomUUID();
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var marcRecord = MarcRecordEntity.builder().externalId(instanceId).build();
    var errorMessage = "error message";

    instancesExportAllStrategy.saveConvertJsonRecordToMarcRecordError(marcRecord, jobExecutionId,
        new IOException(errorMessage));

    var expectedErrorMessage =
        "Error converting json to marc for record 1eaa1eef-1633-4c7e-af09-796315ebc576";
    verify(errorLogService).saveGeneralError(expectedErrorMessage, jobExecutionId);
  }

  private String generateTooLongString() {
    return StringUtils.repeat("abcd", 99999);
  }
}
