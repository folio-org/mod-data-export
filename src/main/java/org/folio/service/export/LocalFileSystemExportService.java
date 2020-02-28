package org.folio.service.export;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.upload.storage.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.List;

@Service
public class LocalFileSystemExportService implements ExportService {
  @Autowired
  private FileStorage fileStorage;

  @Override
  public void export(List<String> marcRecords, FileDefinition fileDefinition) {
    if (CollectionUtils.isNotEmpty(marcRecords) && fileDefinition != null) {
      marcRecords.forEach(marcRecord -> fileStorage.saveFileDataBlocking(marcRecord.getBytes(), fileDefinition));
    }
  }

  @Override
  public void postExport(FileDefinition fileDefinition) {
    // use fileDefinition.getSourcePath() to copy file to S3
  }
}
