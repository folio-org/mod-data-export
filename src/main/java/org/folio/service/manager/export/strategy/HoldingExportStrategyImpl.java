package org.folio.service.manager.export.strategy;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.service.loader.LoadResult;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.manager.export.ExportManagerImpl;
import org.folio.service.manager.export.ExportPayload;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import static java.util.Objects.isNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Service
public class HoldingExportStrategyImpl extends AbstractExportStrategy {

  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void export(ExportPayload exportPayload, Promise<Object> blockingPromise) {
    List<String> identifiers = exportPayload.getIdentifiers();
    FileDefinition fileExportDefinition = exportPayload.getFileExportDefinition();
    MappingProfile defaultMappingProfile = exportPayload.getMappingProfile();
    OkapiConnectionParams params = exportPayload.getOkapiConnectionParams();
    String jobExecutionId = fileExportDefinition.getJobExecutionId();
    SrsLoadResult srsLoadResult = loadSrsMarcRecordsInPartitions(identifiers, exportPayload.getJobExecutionId(), params);
    Pair<List<String>, Integer> marcToExport = getSrsRecordService().transformSrsRecords(defaultMappingProfile, srsLoadResult.getUnderlyingMarcRecords(), jobExecutionId, params, getEntityType());
    getExportService().exportSrsRecord(marcToExport, exportPayload);
    LOGGER.info("Number of holdings without srs record: {}", srsLoadResult.getIdsWithoutSrs());
    if (isNotEmpty(srsLoadResult.getIdsWithoutSrs())) {
      generateRecordsOnTheFly(exportPayload, identifiers, fileExportDefinition, defaultMappingProfile, params, srsLoadResult, marcToExport.getValue());
      blockingPromise.complete();
    } else {
      exportPayload.setExportedRecordsNumber(srsLoadResult.getUnderlyingMarcRecords().size() - marcToExport.getValue());
      exportPayload.setFailedRecordsNumber(identifiers.size() - exportPayload.getExportedRecordsNumber());
      if (exportPayload.isLast()) {
        if (isNull(fileExportDefinition.getSourcePath())) {
          throw new ServiceException(HttpStatus.HTTP_NOT_FOUND, ErrorCode.NO_FILE_GENERATED);
        }
        getExportService().postExport(fileExportDefinition, params.getTenantId());
      }
      blockingPromise.complete();
    }
  }

  private void generateRecordsOnTheFly(ExportPayload exportPayload, List<String> identifiers, FileDefinition fileExportDefinition,
                                       MappingProfile mappingProfile, OkapiConnectionParams params, SrsLoadResult srsLoadResult, int failedSrsRecords) {
    LoadResult holdingsLoadResult = loadHoldingsInPartitions(srsLoadResult.getIdsWithoutSrs(), exportPayload.getJobExecutionId(), params);
    LOGGER.info("Number of holdings, that returned from inventory storage: {}", holdingsLoadResult.getEntities().size());
    int numberOfNotFoundRecords = holdingsLoadResult.getNotFoundEntitiesUUIDs().size();
    LOGGER.info("Number of holdings not found in Inventory Storage: {}", numberOfNotFoundRecords);
    if (numberOfNotFoundRecords > 0) {
      getErrorLogService().populateUUIDsNotFoundErrorLog(exportPayload.getJobExecutionId(), holdingsLoadResult.getNotFoundEntitiesUUIDs(), params.getTenantId());
    }
    Pair<List<String>, Integer> mappedPairResult = getInventoryRecordService().transformHoldingRecords(holdingsLoadResult.getEntities(),
      exportPayload.getJobExecutionId(), mappingProfile, params);
    List<String> mappedMarcRecords = mappedPairResult.getKey();
    int failedRecordsCount = mappedPairResult.getValue();
    getExportService().exportInventoryRecords(mappedMarcRecords, fileExportDefinition, params.getTenantId());
    exportPayload.setExportedRecordsNumber(srsLoadResult.getUnderlyingMarcRecords().size() - failedSrsRecords + mappedMarcRecords.size() - failedRecordsCount);
    exportPayload.setFailedRecordsNumber(identifiers.size() - exportPayload.getExportedRecordsNumber());
    if (exportPayload.isLast()) {
      if (isNull(fileExportDefinition.getSourcePath())) {
        throw new ServiceException(HttpStatus.HTTP_NOT_FOUND, ErrorCode.NO_FILE_GENERATED);
      }
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
      }
    }
  }

  private LoadResult loadHoldingsInPartitions(List<String> holdingIdentifiers, String jobExecutionId, OkapiConnectionParams params) {
    LoadResult loadResult = new LoadResult();
    Lists.partition(holdingIdentifiers, ExportManagerImpl.INVENTORY_LOAD_PARTITION_SIZE).forEach(partition -> {
        LoadResult partitionLoadResult = getRecordLoaderService().getHoldingsById(partition, jobExecutionId, params);
        loadResult.getEntities().addAll(partitionLoadResult.getEntities());
        loadResult.getNotFoundEntitiesUUIDs().addAll(partitionLoadResult.getNotFoundEntitiesUUIDs());
      }
    );
    return loadResult;
  }

  @Override
  protected EntityType getEntityType() {
    return EntityType.HOLDING;
  }
}
