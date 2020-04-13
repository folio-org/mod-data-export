package org.folio.service.export;

import io.vertx.core.json.JsonObject;
import org.folio.TestUtil;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.service.file.storage.FileStorage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(MockitoJUnitRunner.class)
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
    String jsonRecord = new JsonObject(TestUtil.getResourceAsString("json_record.json")).encode();
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
    FileDefinition fileDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    // when
    exportService.postExport(fileDefinition, TENANT);
    // then
    Mockito.verify(exportStorageService, Mockito.times(1)).storeFile(any(FileDefinition.class), anyString());
  }

  @Test(expected = ServiceException.class)
  public void postExport_shouldNotStoreFileFor_Null_FileDefinition() {
    // given
    FileDefinition fileDefinition = null;
    // when
    exportService.postExport(fileDefinition, TENANT);
    // then expect RuntimeException
  }

  @Test(expected = ServiceException.class)
  public void postExport_shouldNotStoreFileFor_Null_SourcePath() {
    // given
    FileDefinition fileDefinition = new FileDefinition()
      .withSourcePath(null);
    // when
    exportService.postExport(fileDefinition, TENANT);
    // then expect RuntimeException
  }
}
