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
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static org.folio.dataexp.util.Constants.DEFAULT_AUTHORITY_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_HOLDINGS_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_INSTANCE_JOB_PROFILE_ID;

@Service
@RequiredArgsConstructor
@Log4j2
public class QuickExportService {

  private final FileDefinitionsService fileDefinitionsService;
  private final DataExportService dataExportService;
  private final JobProfileEntityRepository jobProfileEntityRepository;
  private final ExportIdEntityRepository exportIdEntityRepository;
  private final JobExecutionService jobExecutionService;

  public QuickExportResponse postQuickExport(QuickExportRequest quickExportRequest) {
    var fileDefinition = new FileDefinition().id(UUID.randomUUID()).size(0).fileName("quick-export.csv");
    fileDefinitionsService.postFileDefinition(fileDefinition);
    log.info("Post quick export for job profile {}", quickExportRequest.getJobProfileId());
    dataExportService.postDataExport(getExportRequestFromQuickExportRequest(quickExportRequest, fileDefinition));
    var jobExecution = jobExecutionService.getById(fileDefinition.getJobExecutionId());
    return QuickExportResponse.builder().jobExecutionId(fileDefinition.getJobExecutionId())
      .jobExecutionHrId(jobExecution.getHrId()).build();
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
    var uuids = quickExportRequest.getUuids();
    if (nonNull(uuids)) {
      var batch = new ArrayList<ExportIdEntity>();
      uuids.forEach(instanceId -> {
        var entity = ExportIdEntity.builder().jobExecutionId(fileDefinition
          .getJobExecutionId()).instanceId(instanceId).build();
        batch.add(entity);
      });
      exportIdEntityRepository.saveAll(batch);
    } else {
      log.error("Nothing to export for fileDefinitionId {}: no uuids provided.", fileDefinition.getId());
    }
  }

  private UUID getDefaultJobProfileId(QuickExportRequest quickExportRequest) {
    var predefinedId = quickExportRequest.getJobProfileId();

    if (predefinedId != null) {
      return predefinedId;
    }

    return switch (quickExportRequest.getRecordType()) {
      case INSTANCE -> UUID.fromString(DEFAULT_INSTANCE_JOB_PROFILE_ID);
      case HOLDINGS -> UUID.fromString(DEFAULT_HOLDINGS_JOB_PROFILE_ID);
      case AUTHORITY -> UUID.fromString(DEFAULT_AUTHORITY_JOB_PROFILE_ID);
      default -> {
        log.error("No default job profile found by the following recordType: {}", quickExportRequest.getRecordType());
        throw new DataExportRequestValidationException("No default job profile found by the following recordType: " + quickExportRequest.getRecordType());
      }
    };
  }
}
