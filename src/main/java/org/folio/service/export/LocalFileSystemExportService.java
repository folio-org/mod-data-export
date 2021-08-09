package org.folio.service.export;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
          String instId = getInstanceIdFromMarcRecord(new JsonObject(jsonRecord));
          inventoryClient.getInstancesByIds(Collections.singletonList(instId), jobExecutionId, params, SINGLE_INSTANCE).ifPresent(instancesJson -> {
            JsonArray instances = instancesJson.getJsonArray(INSTANCES);
            errorLogService.saveWithAffectedRecord(instances.getJsonObject(SINGLE_INSTANCE_INDEX), ERROR_MARC_RECORD_CANNOT_BE_CONVERTED.getCode(), jobExecutionId, e, params);
          });
        } catch (RuntimeException e) {
          failedRecords++;
          LOGGER.error("Error during saving srs record to file with content: {}", jsonRecord);
        }
      }
      marcToExport.setValue(failedRecords);
    }
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
