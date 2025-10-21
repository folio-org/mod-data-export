package org.folio.dataexp.service.export.strategies;

import static org.folio.dataexp.service.export.Constants.OUTPUT_BUFFER_SIZE;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.ExportIdEntity;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

/**
 * Abstract base class for all export strategies, providing common logic for
 * exporting any record types.
 */
@Log4j2
@Getter
public abstract class AbstractExportStrategy implements ExportStrategy {

  protected int exportIdsBatch;
  protected String exportTmpStorage;
  private static final String SAVE_ERROR = "{}: Error while saving file {} for job execution ID {}";

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
   * Saves MARC records to local storage for the given export file entity.
   *
   * @param exportFilesEntity the export file entity
   * @param exportRequest the export request
   * @param exportedMarcListener the listener for exported MARC records
   * @return ExportStrategyStatistic containing export statistics
   */
  @Override
  public ExportStrategyStatistic saveOutputToLocalStorage(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportRequest exportRequest,
      ExportedMarcListener exportedMarcListener
  ) {
    var jobExecutionId = exportFilesEntity.getJobExecutionId();
    var exportStatistic = new ExportStrategyStatistic(exportedMarcListener);
    var mappingProfile = getMappingProfile(jobExecutionId);
    var localStorageWriter = createLocalStorageWriter(exportFilesEntity);
    processSlices(
        exportFilesEntity,
        exportStatistic,
        mappingProfile,
        exportRequest,
        localStorageWriter
    );
    try {
      localStorageWriter.close();
    } catch (Exception e) {
      log.error(
          SAVE_ERROR,
          "saveOutputToLocalStorage",
          exportFilesEntity.getFileLocation(),
          jobExecutionId
      );
      exportStatistic.setDuplicatedSrs(0);
      exportStatistic.removeExported();
      long countFailed = exportIdEntityRepository.countExportIds(
          jobExecutionId,
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
   * Processes slices of export IDs for the export file entity. This implementation
   * takes a multithreaded approach. Override to go with non-multithreaded instead
   * if the underlying record gathering is not guaranteed to be thread safe.
   */
  protected void processSlices(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      ExportRequest exportRequest,
      LocalStorageWriter localStorageWriter
  ) {
    var jobExecutionId = exportFilesEntity.getJobExecutionId();
    var tasks = new ArrayList<CompletableFuture<ExportSliceResult>>();
    var page = 0;
    Slice<ExportIdEntity> slice;
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      do {
        final var taskId = page;
        slice = exportIdEntityRepository.getExportIds(
            jobExecutionId,
            exportFilesEntity.getFromId(),
            exportFilesEntity.getToId(),
            PageRequest.of(taskId, exportIdsBatch)
        );
        log.debug("Slice size: {}", slice.getSize());
        var exportIds = slice.getContent().stream()
            .map(ExportIdEntity::getInstanceId)
            .collect(Collectors.toSet());
        tasks.add(
            CompletableFuture.supplyAsync(() ->
                createAndSaveSliceRecords(
                    exportIds,
                    exportStatistic,
                    mappingProfile,
                    exportFilesEntity,
                    exportRequest,
                    taskId
                ),
                executor
            )
        );
        page++;
      } while (slice.hasNext());
    }

    CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();

    tasks.stream()
        .map(CompletableFuture::join)
        .forEach(sliceResult -> {
          copySliceResultToFinal(sliceResult, localStorageWriter, jobExecutionId);
          exportStatistic.aggregate(sliceResult.getStatistic());
        });
  }

  /**
   * Wrap actual create-and-save strategies with boilerplate writer, statistic, and
   * return object setup.
   */
  protected ExportSliceResult createAndSaveSliceRecords(
      Set<UUID> externalIds,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportRequest exportRequest,
      int pageNumber
  ) {
    var jobExecutionId = exportFilesEntity.getJobExecutionId();
    var writer = createLocalStorageWriter(exportFilesEntity, Integer.valueOf(pageNumber));
    var sliceStatistic = new ExportStrategyStatistic(exportStatistic.getExportedMarcListener());
    createAndSaveRecords(
        externalIds,
        sliceStatistic,
        mappingProfile,
        jobExecutionId,
        exportRequest,
        writer
    );
    try {
      writer.close();
    } catch (Exception e) {
      log.error(
          SAVE_ERROR,
          "createAndSaveSliceRecords",
          writer.getPath(),
          jobExecutionId
      );
      sliceStatistic.failAll();
    }
    return new ExportSliceResult(writer.getPath(), sliceStatistic);
  }

  /**
   * Per-strategy implementation of retrieving and writing records to disk within
   * one thread out of a multithreaded approach. Writes and statistics gathering
   * are done on each thread independently of the others and returned for later
   * final aggregation.
   *
   * @param externalIds set of input IDs
   * @param exportStatistic main job statistics
   * @param mappingProfile mapping profile to use
   * @param jobExecutionId job ID
   * @param exportRequest the export request
   * @param writer writes to local storage
   */
  protected abstract void createAndSaveRecords(
      Set<UUID> externalIds,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      UUID jobExecutionId,
      ExportRequest exportRequest,
      LocalStorageWriter writer
  );

  private void copySliceResultToFinal(
      ExportSliceResult sliceResult,
      LocalStorageWriter finalOutput,
      UUID jobExecutionId
  ) {
    try {
      if (sliceResult.getStatistic().getExported() > 0) {
        finalOutput.write(Files.readString(sliceResult.getOutputFile()));
      }
    } catch (Exception e) {
      log.error(
          SAVE_ERROR,
          "copySliceResultToFinal",
          sliceResult.getOutputFile(),
          jobExecutionId
      );
      sliceResult.getStatistic().failAll();
    }
  }

  /**
   * Creates a LocalStorageWriter for the given export file entity.
   */
  protected LocalStorageWriter createLocalStorageWriter(
      JobExecutionExportFilesEntity exportFilesEntity
  ) {
    return createLocalStorageWriter(exportFilesEntity, null);
  }

  /**
   * Creates a LocalStorageWriter for the given export file entity and sliced page number.
   */
  protected LocalStorageWriter createLocalStorageWriter(
      JobExecutionExportFilesEntity exportFilesEntity,
      Integer pageNumber
  ) {
    var fileName = exportFilesEntity.getFileLocation();
    if (pageNumber != null) {
      fileName = "%s-%d".formatted(exportFilesEntity.getFileLocation(), pageNumber);
    }
    return new LocalStorageWriter(
        S3FilePathUtils.getLocalStorageWriterPath(
            exportTmpStorage, fileName
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
  private void setMappingProfileEntityRepository(MappingProfileEntityRepository
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
