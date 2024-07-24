package org.folio.dataexp.service.export.strategies;

import net.minidev.json.JSONObject;
import org.folio.dataexp.domain.entity.AuditInstanceEntity;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.AuditInstanceEntityRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.util.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstancesExportAllStrategyTest {

  @Mock
  private AuditInstanceEntityRepository auditInstanceEntityRepository;
  @Mock
  private InstanceEntityRepository instanceEntityRepository;
  @Mock
  private ErrorLogService errorLogService;

  @InjectMocks
  private InstancesExportAllStrategy instancesExportAllStrategy;

  @Test
  void getIdentifierMessageTest() {
    var auditInstanceEntity = AuditInstanceEntity.builder()
      .id(UUID.randomUUID()).hrid("123").title("title").build();

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of());
    when(auditInstanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(auditInstanceEntity));

    var opt = instancesExportAllStrategy.getIdentifiers(UUID.randomUUID());

    assertTrue(opt.isPresent());
    assertEquals("123", opt.get().getIdentifierHridMessage());

    assertEquals(auditInstanceEntity.getId().toString(), opt.get().getAssociatedJsonObject().getAsString("id"));
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
    assertEquals("Instance with ID : b9d26945-9757-4855-ae6e-fd5d2f7d778e", opt.get().getIdentifierHridMessage());
  }

  @Test
  void saveConvertJsonRecordToMarcRecordErrorIfErrorRecordTooLongAndInstanceDeletedTest() {
    instancesExportAllStrategy.setErrorLogService(errorLogService);

    var auditInstanceEntity = AuditInstanceEntity.builder()
      .id(UUID.randomUUID()).hrid("123").title("title").build();
    var jobExecutionId = UUID.randomUUID();
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var marcRecord = MarcRecordEntity.builder().externalId(instanceId).id(UUID.randomUUID()).build();
    var errorMessage = "Record is too long to be a valid MARC binary record, it's length would be 113937 which is more thatn 99999 bytes 2024";

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of());
    when(auditInstanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(auditInstanceEntity));

    instancesExportAllStrategy.saveConvertJsonRecordToMarcRecordError(marcRecord, jobExecutionId, new IOException(errorMessage));
    verify(errorLogService).saveWithAffectedRecord(isA(JSONObject.class), eq(errorMessage), eq(ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode()), isA(UUID.class));
  }

  @Test
  void saveConvertJsonRecordToMarcRecordErrorIfErrorRecordTooLongAndInstanceNotDeletedTest() {
    instancesExportAllStrategy.setErrorLogService(errorLogService);

    var jobExecutionId = UUID.randomUUID();
    var instance = "{'id' : '1eaa1eef-1633-4c7e-af09-796315ebc576', 'hrid' : 'instHrid', 'title' : 'title'}";
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var instanceEntity = InstanceEntity.builder().jsonb(instance).id(instanceId).build();
    var marcRecord = MarcRecordEntity.builder().externalId(instanceId).build();
    var errorMessage = "Record is too long to be a valid MARC binary record, it's length would be 113937 which is more than 99999 bytes 2024";

    when(instanceEntityRepository.findByIdIn(anySet())).thenReturn(List.of(instanceEntity));

    instancesExportAllStrategy.saveConvertJsonRecordToMarcRecordError(marcRecord, jobExecutionId, new IOException(errorMessage));

    verify(errorLogService).saveWithAffectedRecord(isA(JSONObject.class), eq(errorMessage), eq(ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode()), isA(UUID.class));
  }


  @Test
  void saveConvertJsonRecordToMarcRecordErrorIfNotErrorRecordTooLongTest() {
    instancesExportAllStrategy.setErrorLogService(errorLogService);

    var jobExecutionId = UUID.randomUUID();
    var instanceId = UUID.fromString("1eaa1eef-1633-4c7e-af09-796315ebc576");
    var marcRecord = MarcRecordEntity.builder().externalId(instanceId).build();
    var errorMessage = "error message";

    instancesExportAllStrategy.saveConvertJsonRecordToMarcRecordError(marcRecord, jobExecutionId, new IOException(errorMessage));

    var expectedErrorMessage = "Error converting json to marc for record 1eaa1eef-1633-4c7e-af09-796315ebc576";
    verify(errorLogService).saveGeneralError(expectedErrorMessage, jobExecutionId);
  }
}
