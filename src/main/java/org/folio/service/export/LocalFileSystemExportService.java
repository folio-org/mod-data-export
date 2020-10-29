package org.folio.service.export;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.HttpStatus;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.service.file.storage.FileStorage;
import org.folio.service.logs.ErrorLogService;
import org.folio.util.ErrorCode;
import org.marc4j.MarcJsonReader;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Service
public class LocalFileSystemExportService implements ExportService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  @Qualifier("LocalFileSystemStorage")
  private FileStorage fileStorage;
  @Autowired
  private ExportStorageService exportStorageService;
  @Autowired
  private ErrorLogService errorLogService;

  @Override
  public void exportSrsRecord(List<String> jsonRecords, FileDefinition fileDefinition) {
    if (CollectionUtils.isNotEmpty(jsonRecords) && fileDefinition != null) {
      for (String jsonRecord : jsonRecords) {
        byte[] bytes = convertJsonRecordToMarcRecord(jsonRecord);
        try {
          fileStorage.saveFileDataBlocking(bytes, fileDefinition);
        } catch (RuntimeException e) {
          LOGGER.error("Error during saving srs record to file with content: {}", jsonRecord);
        }
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
    MarcWriter marcStreamWriter = new MarcStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8.name());
    while (marcJsonReader.hasNext()) {
      Record record = marcJsonReader.next();
      marcStreamWriter.write(record);
    }
    return byteArrayOutputStream.toByteArray();
  }


  @Override
  public void exportInventoryRecords(List<String> inventoryRecords, FileDefinition fileDefinition, String tenantId) {
    if (CollectionUtils.isNotEmpty(inventoryRecords) && fileDefinition != null) {
      for (String record : inventoryRecords) {
        byte[] bytes = record.getBytes(StandardCharsets.UTF_8);
        try {
          fileStorage.saveFileDataBlocking(bytes, fileDefinition);
        } catch (RuntimeException e) {
          errorLogService.saveGeneralError("Error during saving record to file", fileDefinition.getJobExecutionId(), tenantId);
          LOGGER.error("Error during saving inventory record to file with content: {}", record);
        }
      }
    }
  }

  @Override
  public void postExport(FileDefinition fileDefinition, String tenantId) {
    if (!isValidFileDefinition(fileDefinition)) {
      if (fileDefinition != null && fileDefinition.getJobExecutionId() != null) {
        errorLogService.saveGeneralError("Invalid export file definition id: " + fileDefinition.getId(), fileDefinition.getJobExecutionId(), tenantId);
      } else {
        errorLogService.saveGeneralError("Export file definition is not valid", EMPTY, tenantId);
      }
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
