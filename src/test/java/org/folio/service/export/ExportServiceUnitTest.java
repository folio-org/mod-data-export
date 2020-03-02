package org.folio.service.export;

import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.upload.storage.FileStorage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
public class ExportServiceUnitTest {
  @Mock
  private FileStorage fileStorage;
  @InjectMocks
  private ExportService exportService = new LocalFileSystemExportService();

  @Test
  public void export_shouldPassExportFor_1000_Records() {
    // given
    List<String> marcRecords = Stream.generate(String::new).limit(1000).collect(Collectors.toList());
    FileDefinition fileDefinition = new FileDefinition();
    Mockito.when(fileStorage.saveFileDataBlocking(any(byte[].class), any(FileDefinition.class))).thenReturn(fileDefinition);
    // when
    exportService.export(marcRecords, fileDefinition);
    // then
    Mockito.verify(fileStorage, Mockito.times(1000)).saveFileDataBlocking(any(byte[].class), any(FileDefinition.class));
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
