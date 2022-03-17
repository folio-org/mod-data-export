package org.folio.service.manager.export.strategy;

import static java.util.Objects.isNull;

import java.lang.invoke.MethodHandles;
import java.util.List;

import io.vertx.core.Promise;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import org.folio.HttpStatus;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.service.loader.SrsLoadResult;
import org.folio.service.manager.export.ExportPayload;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;

@Service
public class AuthorityExportStrategyImpl extends AbstractExportStrategy {

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
    LOGGER.info("Number of authority without srs record: {}", srsLoadResult.getIdsWithoutSrs());
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
  protected EntityType getEntityType() {
    return EntityType.AUTHORITY;
  }
}
