package org.folio.service.manager.export.strategy;

import java.lang.invoke.MethodHandles;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.manager.export.ExportPayload;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.springframework.stereotype.Service;

import io.vertx.core.Promise;

import static java.util.Objects.isNull;

@Service
public class HoldingExportStrategyImpl extends AbstractExportStrategy {

  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  private static final String HOLDING_ID_TYPE = "holding";

  @Override
  public void export(ExportPayload exportPayload, Promise<Object> blockingPromise) {
    List<String> identifiers = exportPayload.getIdentifiers();
    FileDefinition fileExportDefinition = exportPayload.getFileExportDefinition();
    OkapiConnectionParams params = exportPayload.getOkapiConnectionParams();
    SrsLoadResult srsLoadResult = loadSrsMarcRecordsInPartitions(identifiers, exportPayload.getJobExecutionId(), params);
    Pair<List<String>, Integer> marcToExport = getSrsRecordService().transformSrsRecordsForHoldingsExport(srsLoadResult.getUnderlyingMarcRecords());
    getExportService().exportSrsRecord(marcToExport, exportPayload);
    LOGGER.info("Number of holdings without srs: {}", srsLoadResult.getIdsWithoutSrs());

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

  @Override
  protected String getIdType() {
    return HOLDING_ID_TYPE;
  }
}
