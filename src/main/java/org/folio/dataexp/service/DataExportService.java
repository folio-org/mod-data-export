package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.dto.JobExecutionRunBy;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
@Log4j2
public class DataExportService {

  private final FileDefinitionEntityRepository fileDefinitionEntityRepository;
  private final JobExecutionEntityRepository jobExecutionEntityRepository;
  private final JobProfileEntityRepository jobProfileEntityRepository;
  private final ExportIdEntityRepository exportIdEntityRepository;
  private final InputFileProcessor inputFileProcessor;
  private final SlicerProcessor slicerProcessor;
  private final SingleFileProcessorAsync singleFileProcessorAsync;
  private final FolioExecutionContext folioExecutionContext;
  private final UserClient userClient;
  private final DataExportRequestValidator dataExportRequestValidator;

  public void postDataExport(ExportRequest exportRequest) {
    var fileDefinition = fileDefinitionEntityRepository.
      getReferenceById(exportRequest.getFileDefinitionId()).getFileDefinition();
    var jobProfileEntity = jobProfileEntityRepository.getReferenceById(exportRequest.getJobProfileId());
    var jobExecutionEntity = jobExecutionEntityRepository.getReferenceById(fileDefinition.getJobExecutionId());
    try {
      dataExportRequestValidator.validate(exportRequest, fileDefinition, jobProfileEntity.getJobProfile().getMappingProfileId().toString());
    } catch (DataExportRequestValidationException e) {
      updateJobExecutionForPostDataExport(jobExecutionEntity, jobProfileEntity, JobExecution.StatusEnum.FAIL);
      log.error(e.getMessage());
      return;
    }
    log.info("Post data export for file definition {} and job profile {} with job execution {}",
      exportRequest.getFileDefinitionId(), exportRequest.getJobProfileId(), jobExecutionEntity.getId());

    inputFileProcessor.readFile(fileDefinition);
    slicerProcessor.sliceInstancesIds(fileDefinition);

    updateJobExecutionForPostDataExport(jobExecutionEntity, jobProfileEntity, JobExecution.StatusEnum.IN_PROGRESS);
    singleFileProcessorAsync.exportBySingleFile(jobExecutionEntity.getId(), exportRequest.getRecordType());
  }

  private void updateJobExecutionForPostDataExport(JobExecutionEntity jobExecutionEntity, JobProfileEntity jobProfileEntity, JobExecution.StatusEnum jobExecutionStatus) {
    var jobExecution = jobExecutionEntity.getJobExecution();
    jobExecution.setJobProfileId(jobProfileEntity.getJobProfile().getId());
    jobExecution.setJobProfileName(jobProfileEntity.getJobProfile().getName());
    jobExecution.setStatus(jobExecutionStatus);
    var currentDate = new Date();
    jobExecution.setStartedDate(currentDate);
    jobExecution.setLastUpdatedDate(currentDate);

    var user = userClient.getUserById(folioExecutionContext.getUserId().toString());
    var runBy = new JobExecutionRunBy();
    runBy.firstName(user.getPersonal().getFirstName());
    runBy.lastName(user.getPersonal().getLastName());
    jobExecution.setRunBy(runBy);

    long totalExportsIds = exportIdEntityRepository.countByJobExecutionId(jobExecution.getId());
    var jobExecutionProgress = new JobExecutionProgress();
    jobExecutionProgress.setFailed(0);
    jobExecutionProgress.setExported(0);
    jobExecutionProgress.setTotal((int) totalExportsIds);
    jobExecution.setProgress(jobExecutionProgress);

    int hrid = jobExecutionEntityRepository.getHrid();
    jobExecution.setHrId(hrid);

    jobExecutionEntity.setJobProfileId(jobProfileEntity.getId());
    jobExecutionEntity.setStatus(jobExecution.getStatus());
    jobExecutionEntityRepository.save(jobExecutionEntity);
  }
}
