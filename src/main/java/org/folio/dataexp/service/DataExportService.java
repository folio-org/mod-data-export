package org.folio.dataexp.service;

import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;

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
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MarcAuthorityRecordAllRepository;
import org.folio.dataexp.service.validators.DataExportRequestValidator;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
  private final SingleFileProcessorAsync singleFileProcessorAsync;
  private final FolioExecutionContext folioExecutionContext;
  private final UserClient userClient;
  private final DataExportRequestValidator dataExportRequestValidator;
  private final HoldingsRecordEntityRepository holdingsRecordEntityRepository;
  private final InstanceEntityRepository instanceEntityRepository;
  private final MarcAuthorityRecordAllRepository marcAuthorityRecordAllRepository;
  private final ExecutorService executor = Executors.newCachedThreadPool();

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
    var runBy = getRunBy();
    jobExecution.setRunBy(runBy);
    var innerFileName = getDefaultFileName(fileDefinition, jobExecution);
    var innerFile = new JobExecutionExportedFilesInner().fileId(UUID.randomUUID())
      .fileName(FilenameUtils.getName(innerFileName));
    jobExecution.setExportedFiles(Set.of(innerFile));
    try {
      dataExportRequestValidator.validate(exportRequest, fileDefinition, jobProfileEntity.getJobProfile().getMappingProfileId().toString());
    } catch (DataExportRequestValidationException e) {
      updateJobExecutionForPostDataExport(jobExecution, JobExecution.StatusEnum.FAIL, commonExportFails, exportRequest);
      log.error(e.getMessage());
      return;
    }
    log.info("Post data export{} for file definition {} and job profile {} with job execution {}",
        Boolean.TRUE.equals(exportRequest.getAll()) ? " all" : "", exportRequest.getFileDefinitionId(), exportRequest.getJobProfileId(), jobExecution.getId());

    updateJobExecutionForPostDataExport(jobExecution, JobExecution.StatusEnum.IN_PROGRESS, commonExportFails, exportRequest);
    executor.execute(getRunnableWithCurrentFolioContext(() -> {
      if (Boolean.FALSE.equals(exportRequest.getAll()) && Boolean.FALSE.equals(exportRequest.getQuick())) {
        inputFileProcessor.readFile(fileDefinition, commonExportFails, exportRequest.getIdType());
        log.info("File has been read successfully.");
      }
      slicerProcessor.sliceInstancesIds(fileDefinition, exportRequest);
      log.info("Instance IDs have been sliced successfully.");

      updateJobExecutionForPostDataExport(jobExecution, JobExecution.StatusEnum.IN_PROGRESS, commonExportFails, exportRequest);
      singleFileProcessorAsync.exportBySingleFile(jobExecution.getId(), exportRequest, commonExportFails);
    }));
  }

  private void updateJobExecutionForPostDataExport(JobExecution jobExecution, JobExecution.StatusEnum jobExecutionStatus, CommonExportFails commonExportFails, ExportRequest exportRequest) {
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
    updateTotal(exportRequest, jobExecutionProgress, commonExportFails, totalExportsIds);
    jobExecution.setProgress(jobExecutionProgress);

    jobExecutionService.save(jobExecution);
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

  private String getDefaultFileName (FileDefinition fileDefinition, JobExecution jobExecution) {
    var initialFileName = FilenameUtils.getBaseName(fileDefinition.getFileName());
    return String.format("%s-%s.mrc", initialFileName, jobExecution.getHrId());
  }

  private void updateTotal(ExportRequest exportRequest, JobExecutionProgress jobExecutionProgress,
                           CommonExportFails commonExportFails, long totalExportsIds) {
    if (exportRequest.getAll()) {
      if (jobExecutionProgress.getTotal() == 0) {
        if (exportRequest.getIdType() == ExportRequest.IdTypeEnum.HOLDING) {
          jobExecutionProgress.setTotal((int) holdingsRecordEntityRepository.count());
        } else if (exportRequest.getIdType() == ExportRequest.IdTypeEnum.INSTANCE) {
          jobExecutionProgress.setTotal((int) instanceEntityRepository.count());
        } else if (exportRequest.getIdType() == ExportRequest.IdTypeEnum.AUTHORITY) {
          jobExecutionProgress.setTotal((int) marcAuthorityRecordAllRepository.count());
        }
        log.info("Total for export-all {}: {}", exportRequest.getIdType(), jobExecutionProgress.getTotal());
      }
    } else {
      jobExecutionProgress.setTotal((int) totalExportsIds + commonExportFails.getDuplicatedUUIDAmount() + commonExportFails.getInvalidUUIDFormat().size());
    }
  }
}
