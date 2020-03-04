package org.folio.service.export;


import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.upload.storage.FileStorage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
public class ExportServiceUnitTest {
  @Mock
  private FileStorage fileStorage;
  @InjectMocks
  private ExportService exportService = new LocalFileSystemExportService();

  @Test
  public void export_shouldPassExportFor_1_Record() throws IOException {
    // given
    String jsonRecord = new JsonObject(IOUtils.toString(new FileReader("src/test/resources/json_record.json"))).encode();
    FileDefinition fileDefinition = new FileDefinition();
    Mockito.when(fileStorage.saveFileDataBlocking(any(byte[].class), any(FileDefinition.class))).thenReturn(fileDefinition);
    // when
    exportService.export(Collections.singletonList(jsonRecord), fileDefinition);
    // then
    Mockito.verify(fileStorage, Mockito.times(1)).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
  }

  @Test
  public void export_shouldPassExportFor_NULL_Records() {
    // given
    List<String> marcRecords = null;
    FileDefinition fileDefinition = new FileDefinition();
    // when
    exportService.export(marcRecords, fileDefinition);
    // then
    Mockito.verify(fileStorage, Mockito.times(0)).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
  }
}
