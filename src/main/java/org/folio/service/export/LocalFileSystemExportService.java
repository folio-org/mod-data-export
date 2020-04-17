package org.folio.service.export;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.HttpStatus;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.service.file.storage.FileStorage;
import org.folio.util.ErrorCode;
import org.marc4j.MarcJsonReader;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class LocalFileSystemExportService implements ExportService {
  @Autowired
  @Qualifier("LocalFileSystemStorage")
  private FileStorage fileStorage;
  @Autowired
  private ExportStorageService exportStorageService;

  @Override
  public void exportSrsRecord(List<String> jsonRecords, FileDefinition fileDefinition) {
    if (CollectionUtils.isNotEmpty(jsonRecords) && fileDefinition != null) {
      for (String jsonRecord : jsonRecords) {
        byte[] bytes = convertJsonRecordToMarcRecord(jsonRecord);
        fileStorage.saveFileDataBlocking(bytes, fileDefinition);
      }
    }
  }

  /**
   * Converts incoming marc record from json format to raw format
   *
   * @param jsonRecord json record
   * @return array of bytes
   */
  private byte[] convertJsonRecordToMarcRecord(String jsonRecord) {
    MarcReader marcJsonReader = new MarcJsonReader(new ByteArrayInputStream(jsonRecord.getBytes(StandardCharsets.UTF_8)));
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    MarcWriter marcStreamWriter = new MarcStreamWriter(byteArrayOutputStream);
    while (marcJsonReader.hasNext()) {
      Record record = marcJsonReader.next();
      marcStreamWriter.write(record);
    }
    return byteArrayOutputStream.toByteArray();
  }


  @Override
  public void exportInventoryRecords(List<String> inventoryRecords, FileDefinition fileDefinition) {
    if (CollectionUtils.isNotEmpty(inventoryRecords) && fileDefinition != null) {
      for (String record : inventoryRecords) {
        byte[] bytes = record.getBytes(StandardCharsets.UTF_8);
        fileStorage.saveFileDataBlocking(bytes, fileDefinition);
      }
    }
  }

  @Override
  public void postExport(FileDefinition fileDefinition, String tenantId) {
    if (!isValidFileDefinition(fileDefinition)) {
      throw new ServiceException(HttpStatus.HTTP_NOT_FOUND, ErrorCode.NO_FILE_GENERATED);
    }
    exportStorageService.storeFile(fileDefinition, tenantId);
  }

  /**
   * Check if file definition entity and it`s source path is valid
   *
   * @param fileDefinition file definition
   * @return true if file definition is valid
   */
  private boolean isValidFileDefinition(FileDefinition fileDefinition) {
    return fileDefinition != null && fileDefinition.getSourcePath() != null;
  }

}
