package org.folio.service.export;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.upload.storage.FileStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocalFileSystemExportService implements ExportService {
  @Autowired
  @Qualifier("LocalFileSystemStorage")
  private FileStorage fileStorage;

  @Override
  public void export(List<String> marcRecords, FileDefinition fileDefinition) {
    if (CollectionUtils.isNotEmpty(marcRecords) && fileDefinition != null) {
      marcRecords.forEach(marcRecord -> fileStorage.saveFileDataBlocking(marcRecord.getBytes(), fileDefinition));
    }
  }

  @Override
  public void postExport(FileDefinition fileDefinition) {
    // copy file to S3
  }
}
