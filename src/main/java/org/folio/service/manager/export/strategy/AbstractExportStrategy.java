package org.folio.service.manager.export.strategy;

import java.util.List;
import org.folio.clients.InventoryClient;
import org.folio.clients.UsersClient;
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
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import io.vertx.core.Promise;

public abstract class AbstractExportStrategy implements ExportStrategy {

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
  private InventoryClient inventoryClient;
  @Autowired
  private InventoryRecordConverterService inventoryRecordService;

  @Override
  abstract public void export(ExportPayload exportPayload, Promise<Object> blockingPromise);

  /**
   * Loads marc records from SRS by the given instance identifiers
   *
   * @param identifiers instance identifiers
   * @param params      okapi connection parameters
   * @return @see SrsLoadResult
   */
  protected SrsLoadResult loadSrsMarcRecordsInPartitions(List<String> identifiers, String jobExecutionId, OkapiConnectionParams params) {
    SrsLoadResult srsLoadResult = new SrsLoadResult();
    Lists.partition(identifiers, ExportManagerImpl.SRS_LOAD_PARTITION_SIZE).forEach(partition -> {
      SrsLoadResult partitionLoadResult = getRecordLoaderService().loadMarcRecordsBlocking(partition, jobExecutionId, params);
      srsLoadResult.getUnderlyingMarcRecords().addAll(partitionLoadResult.getUnderlyingMarcRecords());
      srsLoadResult.getInstanceIdsWithoutSrs().addAll(partitionLoadResult.getInstanceIdsWithoutSrs());
    });
    return srsLoadResult;
  }

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

  public InventoryClient getInventoryClient() {
    return inventoryClient;
  }

  public InventoryRecordConverterService getInventoryRecordService() {
    return inventoryRecordService;
  }
}
