package org.folio.service.manager.export.strategy;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.clients.UsersClient;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.export.ExportService;
import org.folio.service.job.JobExecutionService;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.manager.export.ExportManagerImpl;
import org.folio.service.manager.export.ExportPayload;
import org.folio.service.mapping.converter.InventoryRecordConverterService;
import org.folio.service.mapping.converter.SrsRecordConverterService;
import org.folio.service.profiles.mappingprofile.MappingProfileService;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.folio.util.ErrorCode.ERROR_DUPLICATE_SRS_RECORD;

public abstract class AbstractExportStrategy implements ExportStrategy {

  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private SrsRecordConverterService srsRecordService;
  @Autowired
  private ExportService exportService;
  @Autowired
  private RecordLoaderService recordLoaderService;
  @Autowired
  private ErrorLogService errorLogService;
  @Autowired
  private MappingProfileService mappingProfileService;
  @Autowired
  private JobExecutionService jobExecutionService;
  @Autowired
  private UsersClient usersClient;
  @Autowired
  private InventoryRecordConverterService inventoryRecordService;

  @Override
  abstract public void export(ExportPayload exportPayload, Promise<Object> blockingPromise);

  /**
   * Loads marc records from SRS by the given identifiers
   *
   * @param identifiers instance identifiers
   * @param params      okapi connection parameters
   * @return @see SrsLoadResult
   */
  protected SrsLoadResult loadSrsMarcRecordsInPartitions(List<String> identifiers, String jobExecutionId,
                                                         OkapiConnectionParams params, ExportPayload exportPayload) {
    SrsLoadResult srsLoadResult = new SrsLoadResult();
    Lists.partition(identifiers, ExportManagerImpl.SRS_LOAD_PARTITION_SIZE).forEach(partition -> {
      SrsLoadResult partitionLoadResult = getRecordLoaderService().loadMarcRecordsBlocking(partition, getEntityType(), jobExecutionId, params);
      srsLoadResult.getUnderlyingMarcRecords().addAll(partitionLoadResult.getUnderlyingMarcRecords());
      srsLoadResult.getIdsWithoutSrs().addAll(partitionLoadResult.getIdsWithoutSrs());
      checkDuplicates(partitionLoadResult.getUnderlyingMarcRecords(), jobExecutionId, params, exportPayload);
    });
    return srsLoadResult;
  }

  private void checkDuplicates(List<JsonObject> underlyingMarcRecords, String jobExecutionId, OkapiConnectionParams params,
                               ExportPayload exportPayload) {
    final Set<String> instanceIds = new HashSet<>();
    underlyingMarcRecords.stream()
      .forEach(rec -> {
        var instanceId = rec.getJsonObject("externalIdsHolder").getString("instanceId");
        if (nonNull(instanceId) && instanceIds.contains(instanceId)) {
          getErrorLogService().saveWithAffectedRecord(rec,
            format(ERROR_DUPLICATE_SRS_RECORD.getDescription(), instanceId),
            ERROR_DUPLICATE_SRS_RECORD.getCode(), jobExecutionId, params);
          exportPayload.setDuplicatedSrs(exportPayload.getDuplicatedSrs() + 1);
          LOGGER.info("Duplicate SRS record found of instance ID {}, total duplicated SRS {}", instanceId,
            exportPayload.getDuplicatedSrs());
        } else {
          instanceIds.add(instanceId);
        }
      });
  }

  protected void postExport(ExportPayload exportPayload, FileDefinition fileExportDefinition, OkapiConnectionParams params) {
    try {
      getExportService().postExport(fileExportDefinition, params.getTenantId());
    } catch (ServiceException exc) {
      getJobExecutionService().getById(exportPayload.getJobExecutionId(), params.getTenantId()).onSuccess(res -> {
        Optional<JsonObject> optionalUser = getUsersClient().getById(fileExportDefinition.getMetadata().getCreatedByUserId(),
          exportPayload.getJobExecutionId(), params);
        if (optionalUser.isPresent()) {
          getJobExecutionService().prepareAndSaveJobForFailedExport(res, fileExportDefinition, optionalUser.get(),
            0, true, params.getTenantId());
        } else {
          LOGGER.error("User which created file export definition does not exist: job failed export cannot be performed.");
        }
      });
      if (getEntityType().equals(EntityType.INSTANCE)) {
        throw new ServiceException(HttpStatus.HTTP_NOT_FOUND, ErrorCode.NO_FILE_GENERATED);
      }
    }
  }

  abstract protected EntityType getEntityType();

  public SrsRecordConverterService getSrsRecordService() {
    return srsRecordService;
  }

  public ExportService getExportService() {
    return exportService;
  }

  public RecordLoaderService getRecordLoaderService() {
    return recordLoaderService;
  }

  public ErrorLogService getErrorLogService() {
    return errorLogService;
  }

  public MappingProfileService getMappingProfileService() {
    return mappingProfileService;
  }

  public JobExecutionService getJobExecutionService() {
    return jobExecutionService;
  }

  public UsersClient getUsersClient() {
    return usersClient;
  }

  public InventoryRecordConverterService getInventoryRecordService() {
    return inventoryRecordService;
  }

  public void handleFailedRecords(ExportPayload exportPayload, List<String> identifiers) {
    var numFailedRecords = identifiers.size() - exportPayload.getExportedRecordsNumber() - exportPayload.getDuplicatedSrs();
    LOGGER.info("Number of failed records found: {}", numFailedRecords);
    exportPayload.setFailedRecordsNumber(Math.abs(numFailedRecords));
  }

  public enum EntityType {
    HOLDING, INSTANCE, AUTHORITY
  }

}
