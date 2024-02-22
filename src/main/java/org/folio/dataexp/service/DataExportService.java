package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionExportedFilesInner;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.dto.JobExecutionRunBy;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.service.validators.DataExportRequestValidator;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class DataExportService {

  private final FileDefinitionEntityRepository fileDefinitionEntityRepository;
  private final JobExecutionService jobExecutionService;
  private final JobProfileEntityRepository jobProfileEntityRepository;
  private final ExportIdEntityRepository exportIdEntityRepository;
  private final InputFileProcessor inputFileProcessor;
  private final SlicerProcessor slicerProcessor;
  private final SingleFileProcessor singleFileProcessor;
  private final FolioExecutionContext folioExecutionContext;
  private final UserClient userClient;
  private final DataExportRequestValidator dataExportRequestValidator;

  public void postDataExport(ExportRequest exportRequest) {
    var commonExportFails = new CommonExportFails();
    var fileDefinitionEntity =  fileDefinitionEntityRepository.
      getReferenceById(exportRequest.getFileDefinitionId());
    var fileDefinition = fileDefinitionEntity.getFileDefinition();
    var jobProfileEntity = jobProfileEntityRepository.getReferenceById(exportRequest.getJobProfileId());
    var jobExecution = jobExecutionService.getById(fileDefinition.getJobExecutionId());
    jobExecution.setJobProfileId(jobProfileEntity.getJobProfile().getId());
    jobExecution.setJobProfileName(jobProfileEntity.getJobProfile().getName());
    jobExecution.setHrId(jobExecutionService.getNextHrid());
    var innerFileName = getDefaultFileName(fileDefinition, jobExecution);
    var innerFile = new JobExecutionExportedFilesInner().fileId(UUID.randomUUID())
      .fileName(FilenameUtils.getName(innerFileName));
    jobExecution.setExportedFiles(Set.of(innerFile));
    try {
      dataExportRequestValidator.validate(exportRequest, fileDefinition, jobProfileEntity.getJobProfile().getMappingProfileId().toString());
    } catch (DataExportRequestValidationException e) {
      updateJobExecutionForPostDataExport(jobExecution, JobExecution.StatusEnum.FAIL, commonExportFails);
      log.error(e.getMessage());
      return;
    }
    log.info("Post data export{} for file definition {} and job profile {} with job execution {}",
        Boolean.TRUE.equals(exportRequest.getAll()) ? " all" : "", exportRequest.getFileDefinitionId(), exportRequest.getJobProfileId(), jobExecution.getId());

    if (Boolean.FALSE.equals(exportRequest.getAll()) && Boolean.FALSE.equals(exportRequest.getQuick())) {
      inputFileProcessor.readFile(fileDefinition, commonExportFails);
      log.info("File has been read successfully.");
    }
    slicerProcessor.sliceInstancesIds(fileDefinition, exportRequest);
    log.info("Instance IDs have been sliced successfully.");

    updateJobExecutionForPostDataExport(jobExecution, JobExecution.StatusEnum.IN_PROGRESS, commonExportFails);
    singleFileProcessor.exportBySingleFile(jobExecution.getId(), exportRequest, commonExportFails);
  }

  private void updateJobExecutionForPostDataExport(JobExecution jobExecution, JobExecution.StatusEnum jobExecutionStatus, CommonExportFails commonExportFails) {
    jobExecution.setStatus(jobExecutionStatus);
    var currentDate = new Date();
    jobExecution.setStartedDate(currentDate);
    jobExecution.setLastUpdatedDate(currentDate);
    if (jobExecutionStatus == JobExecution.StatusEnum.FAIL) jobExecution.setCompletedDate(currentDate);

    var userId = folioExecutionContext.getUserId().toString();
    var user = userClient.getUserById(userId);
    var runBy = new JobExecutionRunBy();
    runBy.firstName(user.getPersonal().getFirstName());
    runBy.lastName(user.getPersonal().getLastName());
    runBy.setUserId(userId);
    jobExecution.setRunBy(runBy);

    long totalExportsIds = exportIdEntityRepository.countByJobExecutionId(jobExecution.getId());
    var jobExecutionProgress = new JobExecutionProgress();
    jobExecutionProgress.setFailed(0);
    jobExecutionProgress.setExported(0);
    jobExecutionProgress.setTotal((int) totalExportsIds + commonExportFails.getDuplicatedUUIDAmount() + commonExportFails.getInvalidUUIDFormat().size());
    jobExecution.setProgress(jobExecutionProgress);

    jobExecutionService.save(jobExecution);
  }

  private String getDefaultFileName(FileDefinition fileDefinition, JobExecution jobExecution) {
    var initialFileName = FilenameUtils.getBaseName(fileDefinition.getFileName());
    return String.format("%s-%s.mrc", initialFileName, jobExecution.getHrId());
  }
}
