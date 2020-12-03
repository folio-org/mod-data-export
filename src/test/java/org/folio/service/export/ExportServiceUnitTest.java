package org.folio.service.export;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.assertj.core.util.Lists;
import org.folio.TestUtil;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.service.file.storage.FileStorage;
import org.folio.service.logs.ErrorLogService;
import org.folio.util.ErrorCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class ExportServiceUnitTest {
  private static final String JOB_EXECUTION_ID = "jobExecutionId";
  private static final String RECORD_CONTENT = "record content";
  private static final String FILE_DEFINITION_ID = "file definition id";

  @Mock
  private FileStorage fileStorage;
  @Mock
  private ExportStorageService exportStorageService;
  @Mock
  private ErrorLogService errorLogService;
  @InjectMocks
  private ExportService exportService = new LocalFileSystemExportService();

  private static final String TENANT = "tenant";

  @Test
  void shouldPassExportFor_1_SrsRecord() {
    // given
    String response = TestUtil.readFileContentFromResources("mockData/srs/get_records_response.json");
    String jsonRecord = new JsonObject(response).getJsonArray("sourceRecords").getJsonObject(0).toString();
    FileDefinition fileDefinition = new FileDefinition();
    when(fileStorage.saveFileDataBlocking(any(byte[].class), any(FileDefinition.class))).thenReturn(fileDefinition);
    // when
    exportService.exportSrsRecord(Collections.singletonList(jsonRecord), fileDefinition);
    // then
    Mockito.verify(fileStorage, Mockito.times(1)).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
  }

  @Test
  void shouldPassExportFor_NULL_SrsRecords() {
    // given
    List<String> marcRecords = null;
    FileDefinition fileDefinition = new FileDefinition();
    // when
    exportService.exportSrsRecord(marcRecords, fileDefinition);
    // then
    Mockito.verify(fileStorage, Mockito.times(0)).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
  }

  @Test
  void shouldPassExportFor_1_InventoryRecord() {
    // given
    String inventoryRecord = "testRecord";
    FileDefinition fileDefinition = new FileDefinition();
    when(fileStorage.saveFileDataBlocking(any(byte[].class), any(FileDefinition.class))).thenReturn(fileDefinition);
    // when
    exportService.exportInventoryRecords(Collections.singletonList(inventoryRecord), fileDefinition, TENANT);
    // then
    Mockito.verify(fileStorage, Mockito.times(1)).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
  }

  @Test
  void shouldPassExportFor_NULL_InventoryRecords() {
    // given
    List<String> inventoryRecords = null;
    FileDefinition fileDefinition = new FileDefinition();
    // when
    exportService.exportInventoryRecords(inventoryRecords, fileDefinition, TENANT);
    // then
    Mockito.verify(fileStorage, Mockito.times(0)).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
  }

  @Test
  void shouldPopulateErrorLog_whenExportInventoryRecordFails() {
    // given
    List<String> inventoryRecords = Lists.newArrayList(RECORD_CONTENT);
    FileDefinition fileDefinition = new FileDefinition();
    fileDefinition.setJobExecutionId(JOB_EXECUTION_ID);
    when(fileStorage.saveFileDataBlocking(any(byte[].class), any(FileDefinition.class))).thenThrow(RuntimeException.class);
    // when
    exportService.exportInventoryRecords(inventoryRecords, fileDefinition, TENANT);
    // then
    verify(errorLogService).saveGeneralError(ErrorCode.ERROR_SAVING_RECORD_TO_FILE.getCode(), JOB_EXECUTION_ID, TENANT);
  }

  @Test
  void postExport_shouldStoreFile() {
    // given
    FileDefinition fileDefinition = new FileDefinition().withSourcePath("generatedBinaryFile.mrc");
    // when
    exportService.postExport(fileDefinition, TENANT);
    // then
    Mockito.verify(exportStorageService, Mockito.times(1)).storeFile(any(FileDefinition.class), anyString());
  }

  @Test
  void postExport_shouldNotStoreFileFor_Null_FileDefinition() {
    // given
    FileDefinition fileDefinition = null;
    // when
    Assertions.assertThrows(ServiceException.class, () -> {
      exportService.postExport(fileDefinition, TENANT);
    });

    verify(errorLogService).saveGeneralError(ErrorCode.INVALID_EXPORT_FILE_DEFINITION.getCode(), "", TENANT);
  }

  @Test
  void postExport_shouldNotStoreFileFor_Null_SourcePath() {
    // given
    FileDefinition fileDefinition = new FileDefinition()
      .withJobExecutionId(JOB_EXECUTION_ID)
      .withId(FILE_DEFINITION_ID)
      .withSourcePath(null);
    // when
    Assertions.assertThrows(ServiceException.class, () -> {
      exportService.postExport(fileDefinition, TENANT);
    });
    // then
    verify(errorLogService).saveGeneralErrorWithMessageValues(ErrorCode.INVALID_EXPORT_FILE_DEFINITION_ID.getCode(), Arrays.asList(FILE_DEFINITION_ID), JOB_EXECUTION_ID, TENANT);
  }
}
