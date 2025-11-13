package org.folio.dataexp.service.export.strategies;

import static org.folio.dataexp.service.export.Constants.OUTPUT_BUFFER_SIZE;

import java.util.UUID;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.service.JobExecutionService;
import org.folio.dataexp.service.export.LocalStorageWriter;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.util.S3FilePathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Abstract base class for all export strategies, providing common logic for
 * exporting any record types.
 */
@Log4j2
@Getter
public abstract class AbstractExportStrategy implements ExportStrategy {

  protected int exportIdsBatch;
  protected String exportTmpStorage;

  protected ExportIdEntityRepository exportIdEntityRepository;
  protected MappingProfileEntityRepository mappingProfileEntityRepository;
  protected JobProfileEntityRepository jobProfileEntityRepository;
  protected JobExecutionService jobExecutionService;

  protected ErrorLogService errorLogService;

  @Value("#{ T(Integer).parseInt('${application.export-ids-batch}')}")
  protected void setExportIdsBatch(int exportIdsBatch) {
    this.exportIdsBatch = exportIdsBatch;
  }

  @Value("${application.export-tmp-storage}")
  protected void setExportTmpStorage(String exportTmpStorage) {
    this.exportTmpStorage = exportTmpStorage;
  }


  /**
   * Saves records to local storage for the given export file entity.
   *
   * @param exportFilesEntity the export file entity
   * @param exportRequest the export request
   * @param exportedRecordsListener the listener for exported records
   * @return ExportStrategyStatistic containing export statistics
   */
  @Override
  public ExportStrategyStatistic saveOutputToLocalStorage(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportRequest exportRequest,
      ExportedRecordsListener exportedRecordsListener
  ) {
    var exportStatistic = new ExportStrategyStatistic(exportedRecordsListener);
    var mappingProfile = getMappingProfile(exportFilesEntity.getJobExecutionId());
    var localStorageWriter = createLocalStorageWriter(exportFilesEntity);
    processSlices(
        exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter
    );
    try {
      localStorageWriter.close();
    } catch (Exception e) {
      log.error(
          "saveOutputToLocalStorage:: Error while saving file {} to local storage"
          + " for job execution {}",
          exportFilesEntity.getFileLocation(), exportFilesEntity.getJobExecutionId()
      );
      exportStatistic.setDuplicatedSrs(0);
      exportStatistic.removeExported();
      long countFailed = exportIdEntityRepository.countExportIds(
          exportFilesEntity.getJobExecutionId(),
          exportFilesEntity.getFromId(),
          exportFilesEntity.getToId()
      );
      exportStatistic.setFailed((int) countFailed);
    }
    return exportStatistic;
  }

  /**
   * Sets the status of the export file entity based on export statistics.
   *
   * @param exportFilesEntity the export file entity
   * @param exportStatistic the export statistics
   */
  @Override
  public void setStatusBaseExportStatistic(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic
  ) {
    if (isCompleted(exportStatistic)) {
      exportFilesEntity.setStatus(JobExecutionExportFilesStatus.COMPLETED);
    }
    if (isCompletedWithErrors(exportStatistic)) {
      exportFilesEntity.setStatus(JobExecutionExportFilesStatus.COMPLETED_WITH_ERRORS);
    }
    if (isFailed(exportStatistic)) {
      exportFilesEntity.setStatus(JobExecutionExportFilesStatus.FAILED);
    }
  }

  private boolean isCompleted(ExportStrategyStatistic exportStatistic) {
    return exportStatistic.getFailed() == 0 && exportStatistic.getExported() > 0;
  }

  private boolean isCompletedWithErrors(ExportStrategyStatistic exportStatistic) {
    return exportStatistic.getFailed() > 0 && exportStatistic.getExported() > 0;
  }

  private boolean isFailed(ExportStrategyStatistic exportStatistic) {
    return exportStatistic.getFailed() >= 0 && exportStatistic.getExported() == 0;
  }

  /**
   * Processes slices of export IDs for the export file entity.
   */
  protected abstract void processSlices(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      ExportRequest exportRequest,
      LocalStorageWriter localStorageWriter
  );

  /**
   * Creates a LocalStorageWriter for the given export file entity.
   */
  protected LocalStorageWriter createLocalStorageWriter(
      JobExecutionExportFilesEntity exportFilesEntity
  ) {
    return new LocalStorageWriter(
        S3FilePathUtils.getLocalStorageWriterPath(
            exportTmpStorage, exportFilesEntity.getFileLocation()
        ),
        OUTPUT_BUFFER_SIZE
    );
  }

  /**
   * Gets the mapping profile for a job execution.
   */
  private MappingProfile getMappingProfile(UUID jobExecutionId) {
    var jobExecution = jobExecutionService.getById(jobExecutionId);
    var jobProfile = jobProfileEntityRepository.getReferenceById(jobExecution.getJobProfileId());
    return mappingProfileEntityRepository
        .getReferenceById(jobProfile.getMappingProfileId())
        .getMappingProfile();
  }

  @Autowired
  private void setExportIdEntityRepository(ExportIdEntityRepository exportIdEntityRepository) {
    this.exportIdEntityRepository = exportIdEntityRepository;
  }

  @Autowired
  private void setJobProfileEntityRepository(JobProfileEntityRepository
      jobProfileEntityRepository) {
    this.jobProfileEntityRepository = jobProfileEntityRepository;
  }

  @Autowired
  protected void setMappingProfileEntityRepository(MappingProfileEntityRepository
        mappingProfileEntityRepository) {
    this.mappingProfileEntityRepository = mappingProfileEntityRepository;
  }

  @Autowired
  private void setJobExecutionService(JobExecutionService jobExecutionService) {
    this.jobExecutionService = jobExecutionService;
  }

  @Autowired
  protected void setErrorLogService(ErrorLogService errorLogService) {
    this.errorLogService = errorLogService;
  }
}
