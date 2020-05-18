package org.folio.service.export;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;
import org.folio.TestUtil;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.service.file.storage.FileStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
public class ExportServiceUnitTest {
  @Mock
  private FileStorage fileStorage;
  @Mock
  private ExportStorageService exportStorageService;
  @InjectMocks
  private ExportService exportService = new LocalFileSystemExportService();

  private static final String TENANT = "tenant";

  @Test
  public void shouldPassExportFor_1_SrsRecord() {
    // given
    String response = TestUtil.readFileContentFromResources("mockData/srs/get_records_response.json");
    String jsonRecord = new JsonObject(response).getJsonArray("records").getJsonObject(0).toString();
    FileDefinition fileDefinition = new FileDefinition();
    Mockito.when(fileStorage.saveFileDataBlocking(any(byte[].class), any(FileDefinition.class))).thenReturn(fileDefinition);
    // when
    exportService.exportSrsRecord(Collections.singletonList(jsonRecord), fileDefinition);
    // then
    Mockito.verify(fileStorage, Mockito.times(1)).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
  }

  @Test
  public void shouldPassExportFor_NULL_SrsRecords() {
    // given
    List<String> marcRecords = null;
    FileDefinition fileDefinition = new FileDefinition();
    // when
    exportService.exportSrsRecord(marcRecords, fileDefinition);
    // then
    Mockito.verify(fileStorage, Mockito.times(0)).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
  }

  @Test
  public void shouldPassExportFor_1_InventoryRecord() {
    // given
    String inventoryRecord = "testRecord";
    FileDefinition fileDefinition = new FileDefinition();
    Mockito.when(fileStorage.saveFileDataBlocking(any(byte[].class), any(FileDefinition.class))).thenReturn(fileDefinition);
    // when
    exportService.exportInventoryRecords(Collections.singletonList(inventoryRecord), fileDefinition);
    // then
    Mockito.verify(fileStorage, Mockito.times(1)).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
  }

  @Test
  public void shouldPassExportFor_NULL_InventoryRecords() {
    // given
    List<String> inventoryRecords = null;
    FileDefinition fileDefinition = new FileDefinition();
    // when
    exportService.exportInventoryRecords(inventoryRecords, fileDefinition);
    // then
    Mockito.verify(fileStorage, Mockito.times(0)).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
  }

  @Test
  public void postExport_shouldStoreFile() {
    // given
    FileDefinition fileDefinition = new FileDefinition().withSourcePath("generatedBinaryFile.mrc");
    // when
    exportService.postExport(fileDefinition, TENANT);
    // then
    Mockito.verify(exportStorageService, Mockito.times(1)).storeFile(any(FileDefinition.class), anyString());
  }

  @Test
  public void postExport_shouldNotStoreFileFor_Null_FileDefinition() {
    // given
    FileDefinition fileDefinition = null;
    // when
    Assertions.assertThrows(ServiceException.class, () -> {
      exportService.postExport(fileDefinition, TENANT);
    });

    // then expect RuntimeException
  }

  @Test
  public void postExport_shouldNotStoreFileFor_Null_SourcePath() {
    // given
    FileDefinition fileDefinition = new FileDefinition()
      .withSourcePath(null);
    // when
    Assertions.assertThrows(ServiceException.class, () -> {
      exportService.postExport(fileDefinition, TENANT);
    });
    // then expect RuntimeException
  }
}
