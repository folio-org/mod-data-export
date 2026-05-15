package org.folio.dataexp.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.folio.dataexp.util.Constants.DEFAULT_AUTHORITY_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_HOLDINGS_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_INSTANCE_JOB_PROFILE_ID;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportAllRequest;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.springframework.stereotype.Service;

/** Service for handling export-all operations. */
@Log4j2
@RequiredArgsConstructor
@Service
public class DataExportAllService {

  private final FileDefinitionsService fileDefinitionsService;
  private final DataExportService dataExportService;

  /**
   * Initiates a data export for all records of a given type.
   *
   * @param exportAllRequest The export-all request.
   */
  public void postDataExportAll(ExportAllRequest exportAllRequest) {
    var fileDefinition =
        new FileDefinition()
            .id(UUID.randomUUID())
            .size(0)
            .fileName(exportAllRequest.getIdType() + "-all.csv");
    fileDefinitionsService.postFileDefinition(fileDefinition);
    log.info("Post data export all for job profile {}", exportAllRequest.getJobProfileId());
    dataExportService.postDataExport(
        getExportRequestFromExportAllRequest(exportAllRequest, fileDefinition));
  }

  /**
   * Builds an ExportRequest from an ExportAllRequest and FileDefinition.
   *
   * @param exportAllRequest The export-all request.
   * @param fileDefinition The file definition.
   * @return The constructed ExportRequest.
   */
  private ExportRequest getExportRequestFromExportAllRequest(
      ExportAllRequest exportAllRequest, FileDefinition fileDefinition) {
    var exportRequest = new ExportRequest();
    exportRequest.setIdType(ExportRequest.IdTypeEnum.valueOf(exportAllRequest.getIdType().name()));
    exportRequest.setJobProfileId(getJobProfileId(exportAllRequest));
    exportRequest.setSuppressedFromDiscovery(exportAllRequest.getSuppressedFromDiscovery());
    exportRequest.setDeletedRecords(exportAllRequest.getDeletedRecords());
    exportRequest.setFileDefinitionId(fileDefinition.getId());
    exportRequest.setAll(true);
    return exportRequest;
  }

  /**
   * Gets the job profile ID from the export-all request, or a default if not provided.
   *
   * @param exportAllRequest The export-all request.
   * @return The job profile ID.
   */
  private UUID getJobProfileId(ExportAllRequest exportAllRequest) {
    if (isNull(exportAllRequest.getJobProfileId())) {
      return getDefaultJobProfileId(exportAllRequest);
    }
    return exportAllRequest.getJobProfileId();
  }

  /**
   * Gets the default job profile ID based on the ID type.
   *
   * @param exportAllRequest The export-all request.
   * @return The default job profile ID.
   */
  private UUID getDefaultJobProfileId(ExportAllRequest exportAllRequest) {
    if (nonNull(exportAllRequest.getIdType())) {
      return switch (exportAllRequest.getIdType()) {
        case INSTANCE -> UUID.fromString(DEFAULT_INSTANCE_JOB_PROFILE_ID);
        case HOLDING -> UUID.fromString(DEFAULT_HOLDINGS_JOB_PROFILE_ID);
        case AUTHORITY -> UUID.fromString(DEFAULT_AUTHORITY_JOB_PROFILE_ID);
      };
    }
    return UUID.fromString(DEFAULT_INSTANCE_JOB_PROFILE_ID);
  }
}
