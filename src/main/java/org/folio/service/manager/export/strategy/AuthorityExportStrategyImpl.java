package org.folio.service.manager.export.strategy;

import static java.util.Objects.isNull;

import java.lang.invoke.MethodHandles;

import io.vertx.core.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import org.folio.HttpStatus;
import org.folio.rest.exceptions.ServiceException;
import org.folio.service.manager.export.ExportPayload;
import org.folio.util.ErrorCode;

@Service
public class AuthorityExportStrategyImpl extends AbstractExportStrategy {

  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void export(ExportPayload exportPayload, Promise<Object> blockingPromise) {
    var params = exportPayload.getOkapiConnectionParams();
    var fileExportDefinition = exportPayload.getFileExportDefinition();
    var identifiers = exportPayload.getIdentifiers();
    var srsLoadResult = loadSrsMarcRecordsInPartitions(identifiers, exportPayload.getJobExecutionId(), params, exportPayload);
    var marcToExport = getSrsRecordService().transformSrsRecords(exportPayload.getMappingProfile(),
      srsLoadResult.getUnderlyingMarcRecords(), fileExportDefinition.getJobExecutionId(), params, getEntityType());
    getExportService().exportSrsRecord(marcToExport, exportPayload);
    LOGGER.info("Number of authority without srs record: {}", srsLoadResult.getIdsWithoutSrs());
    exportPayload.setExportedRecordsNumber(srsLoadResult.getUnderlyingMarcRecords().size() - marcToExport.getValue());
    handleFailedRecords(exportPayload, identifiers);
    if (exportPayload.isLast()) {
      if (isNull(fileExportDefinition.getSourcePath())) {
        throw new ServiceException(HttpStatus.HTTP_NOT_FOUND, ErrorCode.NO_FILE_GENERATED);
      }
      getExportService().postExport(fileExportDefinition, params.getTenantId());
    }
    blockingPromise.complete();
  }

  @Override
  protected EntityType getEntityType() {
    return EntityType.AUTHORITY;
  }
}
