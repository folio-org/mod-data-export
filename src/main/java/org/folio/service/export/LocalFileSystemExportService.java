package org.folio.service.export;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.export.storage.ExportStorageService;
import org.folio.service.file.storage.FileStorage;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.manager.export.ExportPayload;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.marc4j.MarcException;
import org.marc4j.MarcJsonReader;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.util.ErrorCode.ERROR_MARC_RECORD_CANNOT_BE_CONVERTED;

@Service
public class LocalFileSystemExportService implements ExportService {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  private static final String GENERAL_INFO_FIELD_TAG_NUMBER = "999";
  private static final String INDICATOR_VALUE = "f";
  private static final String FIRST_INDICATOR = "ind1";
  private static final String SECOND_INDICATOR = "ind2";
  private static final String INSTANCE_ID_KEY = "i";
  private static final String SUBFIELDS_KEY = "subfields";
  private static final int SINGLE_INSTANCE_INDEX = 0;
  private static final String INSTANCES = "instances";
  private static final int SINGLE_INSTANCE = 1;
  private static final int NUMBER_OF_SYMBOLS_IN_UUID = 36;
  private static final String INSTANCE_FIELD = "\"999\":";
  private static final String INSTANCE_SUBFIELD = "\"i\":\"";

  @Autowired
  @Qualifier("LocalFileSystemStorage")
  private FileStorage fileStorage;
  @Autowired
  private ExportStorageService exportStorageService;
  @Autowired
  private ErrorLogService errorLogService;
  @Autowired
  private InventoryClient inventoryClient;

  @Override
  public void exportSrsRecord(Pair<List<String>, Integer> marcToExport, ExportPayload exportPayload) {
    FileDefinition fileDefinition = exportPayload.getFileExportDefinition();
    String jobExecutionId = exportPayload.getJobExecutionId();
    OkapiConnectionParams params = exportPayload.getOkapiConnectionParams();
    List<String> jsonRecords = marcToExport.getKey();
    if (CollectionUtils.isNotEmpty(jsonRecords) && fileDefinition != null) {
      int failedRecords = 0;
      for (String jsonRecord : jsonRecords) {
        try {
          byte[] bytes = convertJsonRecordToMarcRecord(jsonRecord);
          if (isNotEmpty(bytes)) {
            fileStorage.saveFileDataBlocking(bytes, fileDefinition);
          }
        } catch (MarcException e) {
          failedRecords++;
          ErrorCode errorCode = ERROR_MARC_RECORD_CANNOT_BE_CONVERTED;
          if (isJsonValid(jsonRecord)) {
            String instId = getInstanceIdFromMarcRecord(new JsonObject(jsonRecord));
            handleMarcException(instId, jobExecutionId, params, errorCode, e.getMessage());
          } else {
            handleSpecificExceptionWhenJsonIsInvalid(jsonRecord, jobExecutionId, params, errorCode, e.getMessage());
          }
        } catch (RuntimeException e) {
          failedRecords++;
          LOGGER.error("Error during saving srs record to file with content: {}", jsonRecord);
        }
      }
      marcToExport.setValue(failedRecords);
    }
  }

  private boolean isJsonValid(String jsonRecord) {
    try {
      new JsonObject(jsonRecord);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private void handleSpecificExceptionWhenJsonIsInvalid(String jsonRecord, String jobExecutionId, OkapiConnectionParams params, ErrorCode errorCode, String marcExceptionMessage) {
    String affectedField = StringUtils.substringAfter(marcExceptionMessage, "Member Name: ");
    if (affectedField.isBlank()) {
      affectedField = ": affected field cannot be determined";
    }
    String errorLogMessage = StringUtils.substringBefore(marcExceptionMessage, ";") + ". Field name " + String.join(", ", affectedField);
    String instId = tryToRetrieveInstanceIdWhenJsonIsInvalid(jsonRecord);
    if (!instId.isEmpty()) {
      handleMarcException(instId, jobExecutionId, params, errorCode, errorLogMessage);
    } else {
      errorLogService.saveWithAffectedRecord(new JsonObject(), errorCode.getCode(), jobExecutionId, new MarcException(errorLogMessage), params);
    }
  }

  private String tryToRetrieveInstanceIdWhenJsonIsInvalid(String jsonRecord) {
    String instId = EMPTY;
    try {
      jsonRecord = StringUtils.deleteWhitespace(jsonRecord);
      instId = StringUtils.substringAfterLast(StringUtils.substringAfter(jsonRecord, INSTANCE_FIELD), INSTANCE_SUBFIELD).substring(0, NUMBER_OF_SYMBOLS_IN_UUID);
    } catch (IndexOutOfBoundsException e) {
      // Case when instance id cannot be found.
    }
    return instId;
  }

  private void handleMarcException(String instId, String jobExecutionId, OkapiConnectionParams params, ErrorCode errorCode, String errorLogMessage) {
    inventoryClient.getInstancesWithPrecedingSucceedingTitlesByIds(Collections.singletonList(instId), jobExecutionId, params, SINGLE_INSTANCE).ifPresent(instancesByIds -> {
      JsonArray instances = instancesByIds.getJsonArray(INSTANCES);
      errorLogService.saveWithAffectedRecord(instances.getJsonObject(SINGLE_INSTANCE_INDEX), errorCode.getCode(), jobExecutionId, new MarcException(errorLogMessage), params);
    });
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  private String getInstanceIdFromMarcRecord(JsonObject marcRecord) {
    JsonArray fields = marcRecord.getJsonArray("fields");
    JsonObject instanceIdHolderField = fields.stream()
      .map(JsonObject.class::cast)
      .filter(jsonObject -> {
        if (jsonObject.containsKey(GENERAL_INFO_FIELD_TAG_NUMBER)) {
          JsonObject dataFieldContent = jsonObject.getJsonObject(GENERAL_INFO_FIELD_TAG_NUMBER);
          String firstIndicator = dataFieldContent.getString(FIRST_INDICATOR);
          String secondIndicator = dataFieldContent.getString(SECOND_INDICATOR);
          return StringUtils.isNotBlank(firstIndicator) && StringUtils.isNotBlank(secondIndicator)
            && firstIndicator.equals(secondIndicator) && firstIndicator.equals(INDICATOR_VALUE);
        }
        return false;
      }).findFirst().get();
    JsonArray subfields = instanceIdHolderField.getJsonObject(GENERAL_INFO_FIELD_TAG_NUMBER).getJsonArray(SUBFIELDS_KEY);
    return subfields.stream()
      .map(JsonObject.class::cast)
      .filter(jsonObject -> jsonObject.containsKey(INSTANCE_ID_KEY))
      .findFirst().get().getString(INSTANCE_ID_KEY);
  }

  /**
   * Converts incoming marc record from json format to raw format
   *
   * @param jsonRecord json record
   * @return array of bytes
   */
  private byte[] convertJsonRecordToMarcRecord(String jsonRecord) {
    var byteArrayInputStream = new ByteArrayInputStream(jsonRecord.getBytes(StandardCharsets.UTF_8));
    var byteArrayOutputStream = new ByteArrayOutputStream();
    try (byteArrayInputStream; byteArrayOutputStream) {
      MarcReader marcJsonReader = new MarcJsonReader(byteArrayInputStream);
      MarcWriter marcStreamWriter = new MarcStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8.name());
      try {
        while (marcJsonReader.hasNext()) {
          Record record = marcJsonReader.next();
          marcStreamWriter.write(record);
        }
        // Handle unchecked json parse exception when parser encounters with control character or
        // any other unexpected data.
      } catch (Exception e) {
        throw new MarcException(e.getMessage());
      }
      return byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      return null;
    }
  }


  @Override
  public void exportInventoryRecords(List<String> inventoryRecords, FileDefinition fileDefinition, String tenantId) {
    if (CollectionUtils.isNotEmpty(inventoryRecords) && fileDefinition != null) {
      for (String record : inventoryRecords) {
        byte[] bytes = record.getBytes(StandardCharsets.UTF_8);
        try {
          if (isNotEmpty(bytes)) {
            fileStorage.saveFileDataBlocking(bytes, fileDefinition);
          }
        } catch (RuntimeException e) {
          errorLogService.saveGeneralError(ErrorCode.ERROR_SAVING_RECORD_TO_FILE.getCode(), fileDefinition.getJobExecutionId(), tenantId);
          LOGGER.error("Error during saving inventory record to file with content: {}", record);
        }
      }
    }
  }

  @Override
  public void postExport(FileDefinition fileDefinition, String tenantId) {
    if (!isValidFileDefinition(fileDefinition)) {
      if (fileDefinition != null && fileDefinition.getJobExecutionId() != null) {
        errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.INVALID_EXPORT_FILE_DEFINITION_ID.getCode(), Arrays.asList(fileDefinition.getId()), fileDefinition.getJobExecutionId(), tenantId);
      } else {
        errorLogService.saveGeneralError(ErrorCode.INVALID_EXPORT_FILE_DEFINITION.getCode(), EMPTY, tenantId);
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
