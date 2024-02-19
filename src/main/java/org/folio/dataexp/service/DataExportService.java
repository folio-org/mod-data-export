package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.dto.JobExecutionRunBy;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.service.validators.DataExportRequestValidator;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;

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
  private final ExecutorService executor = Executors.newCachedThreadPool();

  public void postDataExport(ExportRequest exportRequest) {
    var commonExportFails = new CommonExportFails();
    var fileDefinitionEntity =  fileDefinitionEntityRepository.
      getReferenceById(exportRequest.getFileDefinitionId());
    var fileDefinition = fileDefinitionEntity.getFileDefinition();
    var jobProfileEntity = jobProfileEntityRepository.getReferenceById(exportRequest.getJobProfileId());
    var jobExecutionEntity = jobExecutionEntityRepository.getReferenceById(fileDefinition.getJobExecutionId());
    jobExecutionEntity.getJobExecution().setJobProfileId(jobProfileEntity.getJobProfile().getId());
    jobExecutionEntity.getJobExecution().setJobProfileName(jobProfileEntity.getJobProfile().getName());
    jobExecutionEntity.setJobProfileId(jobProfileEntity.getId());

    int hrid = jobExecutionEntityRepository.getHrid();
    var runBy = getRunBy();
    jobExecutionEntity.getJobExecution().setHrId(hrid);
    jobExecutionEntity.getJobExecution().setRunBy(runBy);

    try {
      dataExportRequestValidator.validate(exportRequest, fileDefinition, jobProfileEntity.getJobProfile().getMappingProfileId().toString());
    } catch (DataExportRequestValidationException e) {
      updateJobExecutionForPostDataExport(jobExecutionEntity, JobExecution.StatusEnum.FAIL, commonExportFails);
      log.error(e.getMessage());
      return;
    }
    log.info("Post data export{} for file definition {} and job profile {} with job execution {}",
        Boolean.TRUE.equals(exportRequest.getAll()) ? " all" : "", exportRequest.getFileDefinitionId(), exportRequest.getJobProfileId(), jobExecutionEntity.getId());

    updateJobExecutionForPostDataExport(jobExecutionEntity, JobExecution.StatusEnum.IN_PROGRESS, commonExportFails);
    executor.execute(getRunnableWithCurrentFolioContext(() -> {
      if (Boolean.FALSE.equals(exportRequest.getAll()) && Boolean.FALSE.equals(exportRequest.getQuick())) {
        inputFileProcessor.readFile(fileDefinition, commonExportFails);
        log.info("File has been read successfully.");
      }
      slicerProcessor.sliceInstancesIds(fileDefinition, exportRequest);
      log.info("Instance IDs have been sliced successfully.");

      updateJobExecutionForPostDataExport(jobExecutionEntity, JobExecution.StatusEnum.IN_PROGRESS, commonExportFails);
      singleFileProcessorAsync.exportBySingleFile(jobExecutionEntity.getId(), exportRequest, commonExportFails);
    }));
  }

  private void updateJobExecutionForPostDataExport(JobExecutionEntity jobExecutionEntity, JobExecution.StatusEnum jobExecutionStatus, CommonExportFails commonExportFails) {
    var jobExecution = jobExecutionEntity.getJobExecution();
    jobExecution.setStatus(jobExecutionStatus);
    var currentDate = new Date();
    jobExecution.setStartedDate(currentDate);
    jobExecution.setLastUpdatedDate(currentDate);
    if (jobExecutionStatus == JobExecution.StatusEnum.FAIL) {
      jobExecution.setCompletedDate(currentDate);
    }
    long totalExportsIds = exportIdEntityRepository.countByJobExecutionId(jobExecution.getId());
    var jobExecutionProgress = new JobExecutionProgress();
    jobExecutionProgress.setFailed(0);
    jobExecutionProgress.setExported(0);
    jobExecutionProgress.setTotal((int) totalExportsIds + commonExportFails.getDuplicatedUUIDAmount() + commonExportFails.getInvalidUUIDFormat().size());
    jobExecution.setProgress(jobExecutionProgress);
    jobExecutionEntity.setStatus(jobExecution.getStatus());
    jobExecutionEntityRepository.save(jobExecutionEntity);
  }

  private JobExecutionRunBy getRunBy() {
    var userId = folioExecutionContext.getUserId().toString();
    var user = userClient.getUserById(userId);
    var runBy = new JobExecutionRunBy();
    runBy.firstName(user.getPersonal().getFirstName());
    runBy.lastName(user.getPersonal().getLastName());
    runBy.setUserId(userId);
    return runBy;
  }
}
