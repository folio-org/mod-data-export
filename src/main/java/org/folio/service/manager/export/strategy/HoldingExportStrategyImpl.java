package org.folio.service.manager.export.strategy;

import java.lang.invoke.MethodHandles;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.clients.InventoryClient;
import org.folio.clients.UsersClient;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.export.ExportService;
import org.folio.service.job.JobExecutionService;
import org.folio.service.loader.RecordLoaderService;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.manager.export.ExportManagerImpl;
import org.folio.service.manager.export.ExportPayload;
import org.folio.service.mapping.converter.SrsRecordConverterService;
import org.folio.service.profiles.mappingprofile.MappingProfileService;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import io.vertx.core.Promise;

@Service
public class HoldingExportStrategyImpl extends AbstractExportStrategy {

  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void export(ExportPayload exportPayload, Promise<Object> blockingPromise) {
    List<String> identifiers = exportPayload.getIdentifiers();
    FileDefinition fileExportDefinition = exportPayload.getFileExportDefinition();
    OkapiConnectionParams params = exportPayload.getOkapiConnectionParams();
    getInventoryClient().getInstanceIdsByHoldingIds(identifiers, params).onSuccess(instanceIds -> {
      SrsLoadResult srsLoadResult = loadSrsMarcRecordsInPartitions(instanceIds, exportPayload.getJobExecutionId(), params);
      Pair<List<String>, Integer> marcToExport = getSrsRecordService().transformSrsRecordsForHoldingsExport(srsLoadResult.getUnderlyingMarcRecords());
      getExportService().exportSrsRecord(marcToExport, exportPayload);
      LOGGER.info("Number of holdings without srs: {}", identifiers.size() - srsLoadResult.getUnderlyingMarcRecords().size());

      exportPayload.setExportedRecordsNumber(srsLoadResult.getUnderlyingMarcRecords().size() - marcToExport.getValue());
      exportPayload.setFailedRecordsNumber(srsLoadResult.getUnderlyingMarcRecords().size() - exportPayload.getExportedRecordsNumber());
      if (exportPayload.isLast()) {
        getExportService().postExport(fileExportDefinition, params.getTenantId());
      }
      blockingPromise.complete();
    });
  }

}
