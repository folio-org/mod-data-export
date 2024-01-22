package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.service.validators.DataExportRequestValidator;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@Service
public class DataExportAllService {

  private final FileDefinitionsService fileDefinitionsService;
  private final DataExportService dataExportService;

  public void postDataExportAll(ExportRequest exportRequest) {
    var fileDefinition = new FileDefinition().id(UUID.randomUUID()).size(0).fileName(exportRequest.getIdType() + "-all.csv");
    exportRequest.setFileDefinitionId(fileDefinition.getId());
    fileDefinitionsService.postFileDefinition(fileDefinition);
    log.info("Post data export all for job profile {}", exportRequest.getJobProfileId());
    dataExportService.postDataExport(exportRequest);
  }
}
