package org.folio.service.export;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.clients.InventoryClient;
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
import static org.folio.util.ErrorCode.ERROR_MARC_RECORD_CONTAINS_CONTROL_CHARACTERS;

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
  private static final String CONTROL_CHARACTERS_PATTERN = "\\p{Cntrl}";
  private static final String CONTROL_CHARACTERS_REPLACE_PATTERN = "[\\p{Cntrl}&&[^\n\r]]";
  private static final String BETWEEN_DOUBLE_QUOTES_REPLACE_PATTERN = "\".*?\"";
  private static final String JSON_CHARACTERS_EXCEPT_DOUBLE_QUOTES = ":{}[] ,\r\n";

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
          String[] dataAfterClosingQuote = getDataAfterClosingQuote(jsonRecord);
          if (dataAfterClosingQuote.length != 0) {
            handleDataAfterClosingQuote(dataAfterClosingQuote, jsonRecord, jobExecutionId, params);
          } else if (Pattern.compile(CONTROL_CHARACTERS_PATTERN).matcher(jsonRecord).find()) {
            handleControlCharacters(jsonRecord, jobExecutionId, params);
          } else {
            handleMarcException(new JsonObject(jsonRecord), jobExecutionId, params, ERROR_MARC_RECORD_CANNOT_BE_CONVERTED, e.getMessage());
          }
        } catch (RuntimeException e) {
          failedRecords++;
          LOGGER.error("Error during saving srs record to file with content: {}", jsonRecord);
        }
      }
      marcToExport.setValue(failedRecords);
    }
  }

  private String[] getDataAfterClosingQuote(String jsonRecord) {
    // First, remove all data inside double quotes.
    jsonRecord = jsonRecord.replaceAll(BETWEEN_DOUBLE_QUOTES_REPLACE_PATTERN, EMPTY);
    // Then, split by the rest of possible json characters to allocate only data after a closing double quote.
    return StringUtils.split(jsonRecord, JSON_CHARACTERS_EXCEPT_DOUBLE_QUOTES);
  }

  private void handleControlCharacters(String jsonRecord, String jobExecutionId, OkapiConnectionParams params) {
    final String controlCharacterMarker = UUID.randomUUID().toString();
    ErrorCode errorCode = ERROR_MARC_RECORD_CONTAINS_CONTROL_CHARACTERS;
    // Replace all control characters with markers, otherwise JsonObject cannot be instantiated.
    jsonRecord = jsonRecord.replaceAll(CONTROL_CHARACTERS_REPLACE_PATTERN, controlCharacterMarker);
    findAllAffectedFieldsAndHandleException(jsonRecord, errorCode, controlCharacterMarker, jobExecutionId, params);
  }

  private void handleDataAfterClosingQuote(String[] dataAfterClosingQuote, String jsonRecord, String jobExecutionId, OkapiConnectionParams params) {
    final String dataAfterClosingQuoteMarker = UUID.randomUUID().toString();
    ErrorCode errorCode = ERROR_MARC_RECORD_CONTAINS_CONTROL_CHARACTERS;
    // Remove all data after a closing double quote to make a valid json.
    for (String data: dataAfterClosingQuote) {
      jsonRecord = jsonRecord.replace("\"" + data, dataAfterClosingQuoteMarker + "\"")
        // Needs to handle mix case when record contains control characters as well, however
        // the error message should be the same (see https://issues.folio.org/browse/MDEXP-442).
        .replaceAll(CONTROL_CHARACTERS_REPLACE_PATTERN, dataAfterClosingQuoteMarker);
    }
    findAllAffectedFieldsAndHandleException(jsonRecord, errorCode, dataAfterClosingQuoteMarker, jobExecutionId, params);
  }

  private void findAllAffectedFieldsAndHandleException(String jsonRecord, ErrorCode errorCode, String marker, String jobExecutionId, OkapiConnectionParams params) {
    JsonObject marcRecord = new JsonObject(jsonRecord);
    List<String> affectedFields = new ArrayList<>();
    for (Object field: marcRecord.getJsonArray("fields")) {
      if (field instanceof JsonObject) {
        JsonObject fieldJson = (JsonObject) field;
        fieldJson.fieldNames().forEach(fieldName -> {
          if (fieldJson.getString(fieldName).contains(marker)) {
            affectedFields.add(fieldName);
          }
        });
      }
    }
    String errorLogMessage = errorCode.getDescription() + String.join(", ", affectedFields);
    handleMarcException(marcRecord, jobExecutionId, params, errorCode, errorLogMessage);
  }

  private void handleMarcException(JsonObject marcRecord, String jobExecutionId, OkapiConnectionParams params, ErrorCode errorCode, String errorLogMessage) {
    String instId = getInstanceIdFromMarcRecord(marcRecord);
    inventoryClient.getInstancesByIds(Collections.singletonList(instId), jobExecutionId, params, SINGLE_INSTANCE).ifPresent(instancesByIds -> {
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
        // Handle unchecked json parse exception when parser encounters with control character.
      } catch (Exception e) {
        throw new MarcException(e.getMessage());
      }
      return byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      return null;
    }
    return byteArrayOutputStream.toByteArray();
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
