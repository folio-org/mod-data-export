package org.folio.dataexp.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportDeletedMarcIdsRequest;
import org.folio.dataexp.domain.dto.ExportDeletedMarcIdsResponse;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static java.util.Objects.nonNull;
import static org.folio.dataexp.util.Constants.DEFAULT_INSTANCE_JOB_PROFILE_ID;

@Log4j2
@RequiredArgsConstructor
@Service
public class ExportDeletedMarcIdsService {

  private final MarcDeletedIdsService marcDeletedIdsService;
  private final DataExportService dataExportService;

  public ExportDeletedMarcIdsResponse postExportDeletedMarcIds(ExportDeletedMarcIdsRequest request) {
    log.info("POST export deleted MARC IDs");
    var fileDefinition = marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(nonNull(request) ? request.getFrom() : null,
      nonNull(request) ? request.getTo() : null);
    var exportRequest = ExportRequest.builder().fileDefinitionId(fileDefinition.getId()).jobProfileId(UUID.fromString(DEFAULT_INSTANCE_JOB_PROFILE_ID))
      .all(false).quick(false).build();
    dataExportService.postDataExport(exportRequest);
    return ExportDeletedMarcIdsResponse.builder().jobExecutionId(fileDefinition.getJobExecutionId()).build();
  }
}
