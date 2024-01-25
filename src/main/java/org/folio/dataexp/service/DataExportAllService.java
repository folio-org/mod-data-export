package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportAllRequest;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Log4j2
@RequiredArgsConstructor
@Service
public class DataExportAllService {

  private final FileDefinitionsService fileDefinitionsService;
  private final DataExportService dataExportService;
  private final JobProfileEntityRepository jobProfileEntityRepository;

  public void postDataExportAll(ExportAllRequest exportAllRequest) {
    var fileDefinition = new FileDefinition().id(UUID.randomUUID()).size(0).fileName(exportAllRequest.getIdType() + "-all.csv");
    fileDefinitionsService.postFileDefinition(fileDefinition);
    log.info("Post data export all for job profile {}", exportAllRequest.getJobProfileId());
    dataExportService.postDataExport(getExportRequestFromExportAllRequest(exportAllRequest, fileDefinition));
  }

  private ExportRequest getExportRequestFromExportAllRequest(ExportAllRequest exportAllRequest, FileDefinition fileDefinition) {
    var exportRequest = new ExportRequest();
    exportRequest.setIdType(ExportRequest.IdTypeEnum.valueOf(exportAllRequest.getIdType().name()));
    exportRequest.setJobProfileId(getJobProfileId(exportAllRequest));
    exportRequest.setSuppressedFromDiscovery(exportAllRequest.getSuppressedFromDiscovery());
    exportRequest.setDeletedRecords(exportAllRequest.getDeletedRecords());
    exportRequest.setFileDefinitionId(fileDefinition.getId());
    exportRequest.setAll(true);
    return exportRequest;
  }

  private UUID getJobProfileId(ExportAllRequest exportAllRequest) {
    if (isNull(exportAllRequest.getJobProfileId())) {
      return getDefaultJobProfileId(exportAllRequest);
    }
    return exportAllRequest.getJobProfileId();
  }

  private UUID getDefaultJobProfileId(ExportAllRequest exportAllRequest) {
    if (nonNull(exportAllRequest.getIdType())) {
      return jobProfileEntityRepository.findIdOfDefaultJobProfileByName(exportAllRequest.getIdType().getValue()).get(0);
    }
    return jobProfileEntityRepository.findIdOfDefaultJobProfileByName(ExportRequest.IdTypeEnum.INSTANCE.getValue()).get(0);
  }
}
