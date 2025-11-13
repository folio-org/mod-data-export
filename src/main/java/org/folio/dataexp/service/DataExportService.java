package org.folio.dataexp.service;

import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;

import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.repository.MarcAuthorityRecordAllRepository;
import org.folio.dataexp.service.validators.DataExportRequestValidator;
import org.folio.dataexp.util.Constants;
import org.folio.dataexp.util.S3FilePathUtils;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

/**
 * Service for handling data export operations.
 */
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
  private final MappingProfileEntityRepository mappingProfileEntityRepository;
  private final ExecutorService executor = Executors.newCachedThreadPool();

  /**
   * Initiates a data export operation.
   *
   * @param exportRequest The export request.
   */
  public void postDataExport(ExportRequest exportRequest) {
    var fileDefinitionEntity =  fileDefinitionEntityRepository
        .getReferenceById(exportRequest.getFileDefinitionId());
    var fileDefinition = fileDefinitionEntity.getFileDefinition();
    var jobProfileEntity = jobProfileEntityRepository.getReferenceById(
        exportRequest.getJobProfileId());
    var mappingProfileEntity = mappingProfileEntityRepository.getReferenceById(
        jobProfileEntity.getMappingProfileId());
    var jobExecution = jobExecutionService.getById(fileDefinition.getJobExecutionId());
    jobExecution.setJobProfileId(jobProfileEntity.getJobProfile().getId());
    jobExecution.setJobProfileName(jobProfileEntity.getJobProfile().getName());
    jobExecution.setHrId(jobExecutionService.getNextHrid());
    jobExecution.setStartedDate(new Date());
    var runBy = getRunBy();
    jobExecution.setRunBy(runBy);
    var innerFileName = getDefaultFileName(fileDefinition, jobExecution,
        mappingProfileEntity.getFormat());
    var innerFile = new JobExecutionExportedFilesInner().fileId(UUID.randomUUID())
        .fileName(FilenameUtils.getName(innerFileName));
    jobExecution.setExportedFiles(Set.of(innerFile));
    try {
      dataExportRequestValidator.validate(
          exportRequest,
          fileDefinition,
          jobProfileEntity.getJobProfile().getMappingProfileId().toString()
      );
    } catch (DataExportRequestValidationException e) {
      updateJobExecutionForPostDataExport(
          jobExecution,
          JobExecution.StatusEnum.FAIL,
          exportRequest
      );
      log.error(e.getMessage());
      return;
    }
    log.info(
        "Post data export{} for file definition {} and job profile {} with job execution {}",
        Boolean.TRUE.equals(exportRequest.getAll()) ? " all" : "",
        exportRequest.getFileDefinitionId(),
        exportRequest.getJobProfileId(),
        jobExecution.getId()
    );

    updateJobExecutionForPostDataExport(
        jobExecution,
        JobExecution.StatusEnum.IN_PROGRESS,
        exportRequest
    );
    var commonExportFails = new CommonExportStatistic();
    executor.execute(getRunnableWithCurrentFolioContext(() -> {
      if (Boolean.FALSE.equals(exportRequest.getAll())
            && Boolean.FALSE.equals(exportRequest.getQuick())) {
        inputFileProcessor.readFile(fileDefinition, commonExportFails, exportRequest.getIdType());
        log.info("File has been read successfully.");
      }
      slicerProcessor.sliceInstancesIds(fileDefinition, exportRequest,
          mappingProfileEntity.getFormat());
      log.info("Instance IDs have been sliced successfully.");

      updateJobExecutionForPostDataExport(
          jobExecution,
          JobExecution.StatusEnum.IN_PROGRESS,
          exportRequest
      );
      singleFileProcessorAsync.exportBySingleFile(
          jobExecution.getId(),
          exportRequest,
          commonExportFails
      );
    }));
  }

  /**
   * Updates the job execution status and progress for a data export.
   *
   * @param jobExecution The job execution.
   * @param jobExecutionStatus The status to set.
   * @param exportRequest The export request.
   */
  private void updateJobExecutionForPostDataExport(
      JobExecution jobExecution,
      JobExecution.StatusEnum jobExecutionStatus,
      ExportRequest exportRequest
  ) {
    jobExecution.setStatus(jobExecutionStatus);
    var currentDate = new Date();
    jobExecution.setLastUpdatedDate(currentDate);
    if (jobExecutionStatus == JobExecution.StatusEnum.FAIL) {
      jobExecution.setCompletedDate(currentDate);
    }

    var jobExecutionProgress = jobExecutionService.getById(jobExecution.getId()).getProgress();
    if (jobExecutionProgress == null) {
      jobExecutionProgress = new JobExecutionProgress();
      jobExecution.setProgress(jobExecutionProgress);
    }
    updateTotal(exportRequest, jobExecution.getId(), jobExecutionProgress);
    jobExecution.setProgress(jobExecutionProgress);

    jobExecutionService.save(jobExecution);
  }

  /**
   * Gets the user information for the runBy field.
   *
   * @return JobExecutionRunBy object.
   */
  private JobExecutionRunBy getRunBy() {
    var userId = folioExecutionContext.getUserId().toString();
    var user = userClient.getUserById(userId);
    var runBy = new JobExecutionRunBy();
    runBy.firstName(user.getPersonal().getFirstName());
    runBy.lastName(user.getPersonal().getLastName());
    runBy.setUserId(userId);
    return runBy;
  }

  /**
   * Gets the default file name for the export.
   *
   * @param fileDefinition The file definition.
   * @param jobExecution The job execution.
   * @param outputFormat The file output format.
   * @return The file name.
   */
  private String getDefaultFileName(FileDefinition fileDefinition, JobExecution jobExecution,
      String outputFormat) {
    var initialFileName = FilenameUtils.getBaseName(fileDefinition.getFileName());
    var fileSuffix = S3FilePathUtils.getFileSuffixFromOutputFormat(outputFormat);
    return String.format(Constants.FILE_NAME_FORMAT, initialFileName, jobExecution.getHrId(),
        fileSuffix);
  }

  /**
   * Updates the total count in job execution progress based on the export request.
   *
   * @param exportRequest The export request.
   * @param jobExecutionId The job execution ID.
   * @param jobExecutionProgress The job execution progress.
   */
  private void updateTotal(
      ExportRequest exportRequest,
      UUID jobExecutionId,
      JobExecutionProgress jobExecutionProgress
  ) {
    if (Boolean.TRUE.equals(exportRequest.getAll())) {
      if (jobExecutionProgress.getTotal() == 0) {
        if (exportRequest.getIdType() == ExportRequest.IdTypeEnum.HOLDING) {
          jobExecutionProgress.setTotal((int) holdingsRecordEntityRepository.count());
        } else if (exportRequest.getIdType() == ExportRequest.IdTypeEnum.INSTANCE) {
          jobExecutionProgress.setTotal((int) instanceEntityRepository.count());
        } else if (exportRequest.getIdType() == ExportRequest.IdTypeEnum.AUTHORITY) {
          jobExecutionProgress.setTotal((int) marcAuthorityRecordAllRepository.count());
        }
        log.info(
            "Total for export-all {}: {}",
            exportRequest.getIdType(),
            jobExecutionProgress.getTotal()
        );
      }
    } else if (Boolean.TRUE.equals(exportRequest.getQuick())) {
      long totalExportsIds = exportIdEntityRepository.countByJobExecutionId(jobExecutionId);
      jobExecutionProgress.setTotal((int) totalExportsIds);
    }
  }
}
