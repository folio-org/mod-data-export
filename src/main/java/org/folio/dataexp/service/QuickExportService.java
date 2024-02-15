package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.QuickExportRequest;
import org.folio.dataexp.domain.dto.QuickExportResponse;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class QuickExportService {

  private final FileDefinitionsService fileDefinitionsService;
  private final DataExportService dataExportService;
  private final JobProfileEntityRepository jobProfileEntityRepository;
  private final ExportIdEntityRepository exportIdEntityRepository;
  private final JobExecutionEntityRepository jobExecutionEntityRepository;

  public QuickExportResponse postQuickExport(QuickExportRequest quickExportRequest) {
    var fileDefinition = new FileDefinition().id(UUID.randomUUID()).size(0).fileName(quickExportRequest.getType() + "-quick.csv");
    fileDefinitionsService.postFileDefinition(fileDefinition);
    log.info("Post quick export for job profile {}", quickExportRequest.getJobProfileId());
    dataExportService.postDataExport(getExportRequestFromQuickExportRequest(quickExportRequest, fileDefinition));
    log.info("Post quick export after");
    var jobExecution = jobExecutionEntityRepository.getReferenceById(fileDefinition.getJobExecutionId());
    log.info("Job execution after post quick: {}", jobExecution);
    return QuickExportResponse.builder().jobExecutionId(fileDefinition.getJobExecutionId())
      .jobExecutionHrId(jobExecution.getJobExecution().getHrId()).build();
  }

  private ExportRequest getExportRequestFromQuickExportRequest(QuickExportRequest quickExportRequest, FileDefinition fileDefinition) {
    var exportRequest = new ExportRequest();
    saveBatch(quickExportRequest, fileDefinition);
    exportRequest.setJobProfileId(getDefaultJobProfileId(quickExportRequest));
    exportRequest.setRecordType(ExportRequest.RecordTypeEnum.fromValue(quickExportRequest.getRecordType().getValue()));
    exportRequest.setFileDefinitionId(fileDefinition.getId());
    exportRequest.setQuick(true);
    exportRequest.setIdType(ExportRequest.IdTypeEnum.fromValue(quickExportRequest.getRecordType().getValue().toLowerCase()));
    return exportRequest;
  }

  private void saveBatch(QuickExportRequest quickExportRequest, FileDefinition fileDefinition) {
    var batch = new ArrayList<ExportIdEntity>();
    quickExportRequest.getUuids().forEach(instanceId -> {
      var entity = ExportIdEntity.builder().jobExecutionId(fileDefinition
        .getJobExecutionId()).instanceId(instanceId).build();
      batch.add(entity);
    });
    exportIdEntityRepository.saveAll(batch);
  }

  private UUID getDefaultJobProfileId(QuickExportRequest quickExportRequest) {
    var ids = jobProfileEntityRepository.findIdOfDefaultJobProfileByName(quickExportRequest.getRecordType().getValue().toLowerCase());
    if (ids.isEmpty()) {
      log.error("No default job profile found by the following recordType: {}", quickExportRequest.getRecordType());
      throw new DataExportRequestValidationException("No default job profile found by the following recordType: " + quickExportRequest.getRecordType());
    }
    if (ids.size() > 1) {
      log.warn("More than 1 job profile found by the following recordType: {}, only first one will be used: {}",
        quickExportRequest.getRecordType(), ids.get(0));
    }
    return ids.get(0);
  }
}
