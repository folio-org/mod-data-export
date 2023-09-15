package org.folio.service.manager.export.strategy;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.service.loader.LoadResult;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.manager.export.ExportManagerImpl;
import org.folio.service.manager.export.ExportPayload;
import org.folio.service.profiles.mappingprofile.MappingProfileServiceImpl;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import io.vertx.core.Promise;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

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
      SrsLoadResult srsLoadResult = loadSrsMarcRecordsInPartitions(identifiers, exportPayload.getJobExecutionId(), params, exportPayload);
      LOGGER.info("Records that are not present in SRS: {}", srsLoadResult.getIdsWithoutSrs());
      Pair<List<String>, Integer> marcToExport = getSrsRecordService().transformSrsRecords(mappingProfile, srsLoadResult.getUnderlyingMarcRecords(),
        exportPayload.getJobExecutionId(), params, getEntityType());
      getExportService().exportSrsRecord(marcToExport, exportPayload);
      LOGGER.info("Number of instances not found in SRS: {}", srsLoadResult.getIdsWithoutSrs().size());
      if (isNotEmpty(srsLoadResult.getIdsWithoutSrs())) {


        // FOLIO



        // MARC
        getMappingProfileService().getDefaultInstanceMappingProfile(params)
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
        handleFailedRecords(exportPayload, identifiers);
        if (exportPayload.isLast()) {
          getExportService().postExport(fileExportDefinition, params.getTenantId());
        }
        blockingPromise.complete();
      }
    } else {
      SrsLoadResult srsLoadResult = new SrsLoadResult();
      srsLoadResult.setIdsWithoutSrs(identifiers);
      generateRecordsOnTheFly(exportPayload, identifiers, fileExportDefinition, mappingProfile, params, srsLoadResult, 0);
      blockingPromise.complete();
    }
  }

  private void generateRecordsOnTheFly(ExportPayload exportPayload, List<String> identifiers, FileDefinition fileExportDefinition,
                                       MappingProfile mappingProfile, OkapiConnectionParams params, SrsLoadResult srsLoadResult, int failedSrsRecords) {


    LoadResult instances = loadInventoryInstancesInPartitions(srsLoadResult.getIdsWithoutSrs(), exportPayload.getJobExecutionId(), params);



    LOGGER.info("Number of instances, that returned from inventory storage: {}", instances.getEntities().size());
    int numberOfNotFoundRecords = instances.getNotFoundEntitiesUUIDs().size();
    LOGGER.info("Number of instances not found in Inventory Storage: {}", numberOfNotFoundRecords);
    if (numberOfNotFoundRecords > 0) {
      getErrorLogService().populateUUIDsNotFoundErrorLog(exportPayload.getJobExecutionId(), instances.getNotFoundEntitiesUUIDs(), params.getTenantId());
    }
    Pair<List<String>, Integer> mappedPairResult = getInventoryRecordService().transformInstanceRecords(instances.getEntities(),
      exportPayload.getJobExecutionId(), mappingProfile, params);
    List<String> mappedMarcRecords = mappedPairResult.getKey();
    int failedRecordsCount = mappedPairResult.getValue();
    getExportService().exportInventoryRecords(mappedMarcRecords, fileExportDefinition, params.getTenantId());
    exportPayload.setExportedRecordsNumber(srsLoadResult.getUnderlyingMarcRecords().size() - failedSrsRecords + mappedMarcRecords.size() - failedRecordsCount);
    handleFailedRecords(exportPayload, identifiers);
    if (exportPayload.isLast()) {
      postExport(exportPayload, fileExportDefinition, params);
    }
  }

  /**
   * Loads instances from Inventory by the given identifiers
   *
   * @param singleInstanceIdentifiers identifiers of instances that do not have underlying srs
   * @param params                    okapi connection parameters
   * @return list of instances
   */
  private LoadResult loadInventoryInstancesInPartitions(List<String> singleInstanceIdentifiers, String jobExecutionId, OkapiConnectionParams params) {
    LoadResult loadResult = new LoadResult();
    Lists.partition(singleInstanceIdentifiers, ExportManagerImpl.INVENTORY_LOAD_PARTITION_SIZE).forEach(partition -> {
        LoadResult partitionLoadResult = getRecordLoaderService().loadInventoryInstancesBlocking(partition, jobExecutionId, params, ExportManagerImpl.INVENTORY_LOAD_PARTITION_SIZE);
        loadResult.getEntities().addAll(partitionLoadResult.getEntities());
        loadResult.getNotFoundEntitiesUUIDs().addAll(partitionLoadResult.getNotFoundEntitiesUUIDs());
      }
    );
    return loadResult;
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

  @Override
  protected EntityType getEntityType() {
    return EntityType.INSTANCE;
  }

}
