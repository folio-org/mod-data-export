package org.folio.service.manager.export.strategy;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.google.common.collect.Lists;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.folio.HttpStatus;
import org.folio.clients.UsersClient;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.service.export.ExportService;
import org.folio.service.job.JobExecutionService;
import org.folio.service.loader.InventoryLoadResult;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.manager.export.ExportManagerImpl;
import org.folio.service.manager.export.ExportPayload;
import org.folio.service.mapping.converter.InventoryRecordConverterService;
import org.folio.service.mapping.converter.SrsRecordConverterService;
import org.folio.service.profiles.mappingprofile.MappingProfileService;
import org.folio.service.profiles.mappingprofile.MappingProfileServiceImpl;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class InstanceExportStrategyImpl extends AbstractExportStrategy {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void export(ExportPayload exportPayload, Promise<Object> blockingPromise) {
    List<String> identifiers = exportPayload.getIdentifiers();
    FileDefinition fileExportDefinition = exportPayload.getFileExportDefinition();
    MappingProfile mappingProfile = exportPayload.getMappingProfile();
    OkapiConnectionParams params = exportPayload.getOkapiConnectionParams();

    if (mappingProfile.getRecordTypes().contains(RecordType.SRS) || MappingProfileServiceImpl.isDefaultInstanceProfile(mappingProfile.getId())) {
      SrsLoadResult srsLoadResult = loadSrsMarcRecordsInPartitions(identifiers, exportPayload.getJobExecutionId(), params);
      LOGGER.info("Records that are not present in SRS: {}", srsLoadResult.getInstanceIdsWithoutSrs());
      Pair<List<String>, Integer> marcToExport = getSrsRecordService().transformSrsRecords(mappingProfile, srsLoadResult.getUnderlyingMarcRecords(),
        exportPayload.getJobExecutionId(), params);
      getExportService().exportSrsRecord(marcToExport, exportPayload);
      LOGGER.info("Number of instances not found in SRS: {}", srsLoadResult.getInstanceIdsWithoutSrs().size());
      if (isNotEmpty(srsLoadResult.getInstanceIdsWithoutSrs())) {
        getMappingProfileService().getDefault(params)
          .onSuccess(defaultMappingProfile -> {
            defaultMappingProfile = appendHoldingsAndItemTransformations(mappingProfile, defaultMappingProfile);
            generateRecordsOnTheFly(exportPayload, identifiers, fileExportDefinition, defaultMappingProfile, params, srsLoadResult, marcToExport.getValue());
            blockingPromise.complete();
          })
          .onFailure(ar -> {
            LOGGER.error("Failed to fetch default mapping profile");
            getErrorLogService().saveGeneralError(ErrorCode.DEFAULT_MAPPING_PROFILE_NOT_FOUND.getCode(), exportPayload.getJobExecutionId(), params.getTenantId());
            throw new ServiceException(HttpStatus.HTTP_INTERNAL_SERVER_ERROR, ErrorCode.DEFAULT_MAPPING_PROFILE_NOT_FOUND);
          });
      } else {
        exportPayload.setExportedRecordsNumber(srsLoadResult.getUnderlyingMarcRecords().size() - marcToExport.getValue());
        exportPayload.setFailedRecordsNumber(identifiers.size() - exportPayload.getExportedRecordsNumber());
        if (exportPayload.isLast()) {
          getExportService().postExport(fileExportDefinition, params.getTenantId());
        }
        blockingPromise.complete();
      }
    } else {
      SrsLoadResult srsLoadResult = new SrsLoadResult();
      srsLoadResult.setInstanceIdsWithoutSrs(identifiers);
      generateRecordsOnTheFly(exportPayload, identifiers, fileExportDefinition, mappingProfile, params, srsLoadResult, 0);
      blockingPromise.complete();
    }
  }

  private void generateRecordsOnTheFly(ExportPayload exportPayload, List<String> identifiers, FileDefinition fileExportDefinition,
                                       MappingProfile mappingProfile, OkapiConnectionParams params, SrsLoadResult srsLoadResult, int failedSrsRecords) {
    InventoryLoadResult instances = loadInventoryInstancesInPartitions(srsLoadResult.getInstanceIdsWithoutSrs(), exportPayload.getJobExecutionId(), params);
    LOGGER.info("Number of instances, that returned from inventory storage: {}", instances.getInstances().size());
    int numberOfNotFoundRecords = instances.getNotFoundInstancesUUIDs().size();
    LOGGER.info("Number of instances not found in Inventory Storage: {}", numberOfNotFoundRecords);
    if (numberOfNotFoundRecords > 0) {
      getErrorLogService().populateUUIDsNotFoundErrorLog(exportPayload.getJobExecutionId(), instances.getNotFoundInstancesUUIDs(), params.getTenantId());
    }
    Pair<List<String>, Integer> mappedPairResult = getInventoryRecordService().transformInventoryRecords(instances.getInstances(),
      exportPayload.getJobExecutionId(), mappingProfile, params);
    List<String> mappedMarcRecords = mappedPairResult.getKey();
    int failedRecordsCount = mappedPairResult.getValue();
    getExportService().exportInventoryRecords(mappedMarcRecords, fileExportDefinition, params.getTenantId());
    exportPayload.setExportedRecordsNumber(srsLoadResult.getUnderlyingMarcRecords().size() - failedSrsRecords + mappedMarcRecords.size() - failedRecordsCount);
    exportPayload.setFailedRecordsNumber(identifiers.size() - exportPayload.getExportedRecordsNumber());
    if (exportPayload.isLast()) {
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
        throw new ServiceException(HttpStatus.HTTP_NOT_FOUND, ErrorCode.NO_FILE_GENERATED);
      }
    }
  }

  /**
   * Loads instances from Inventory by the given identifiers
   *
   * @param singleInstanceIdentifiers identifiers of instances that do not have underlying srs
   * @param params                    okapi connection parameters
   * @return list of instances
   */
  private InventoryLoadResult loadInventoryInstancesInPartitions(List<String> singleInstanceIdentifiers, String jobExecutionId, OkapiConnectionParams params) {
    InventoryLoadResult inventoryLoadResult = new InventoryLoadResult();
    Lists.partition(singleInstanceIdentifiers, ExportManagerImpl.INVENTORY_LOAD_PARTITION_SIZE).forEach(partition -> {
        InventoryLoadResult partitionLoadResult = getRecordLoaderService().loadInventoryInstancesBlocking(partition, jobExecutionId, params, ExportManagerImpl.INVENTORY_LOAD_PARTITION_SIZE);
        inventoryLoadResult.getInstances().addAll(partitionLoadResult.getInstances());
        inventoryLoadResult.getNotFoundInstancesUUIDs().addAll(partitionLoadResult.getNotFoundInstancesUUIDs());
      }
    );
    return inventoryLoadResult;
  }

  /**
   * Append holdings/item transformations to default mapping profile
   *
   * @param mappingProfile        custom mapping profile
   * @param defaultMappingProfile default mapping profile
   * @return default mapping profile
   */
  private MappingProfile appendHoldingsAndItemTransformations(MappingProfile mappingProfile, MappingProfile defaultMappingProfile) {
    if (isNotEmpty(mappingProfile.getTransformations())) {
      List<RecordType> updatedRecordTypes = new ArrayList<>(defaultMappingProfile.getRecordTypes());
      if (mappingProfile.getRecordTypes().contains(RecordType.HOLDINGS)) {
        updatedRecordTypes.add(RecordType.HOLDINGS);
      }
      if (mappingProfile.getRecordTypes().contains(RecordType.ITEM)) {
        updatedRecordTypes.add(RecordType.ITEM);
      }
      defaultMappingProfile.setRecordTypes(updatedRecordTypes);
      defaultMappingProfile.setTransformations(mappingProfile.getTransformations());
    }
    return defaultMappingProfile;
  }

}
