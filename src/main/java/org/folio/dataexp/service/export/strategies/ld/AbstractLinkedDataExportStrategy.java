package org.folio.dataexp.service.export.strategies.ld;

import static org.folio.dataexp.service.export.Constants.OUTPUT_BUFFER_SIZE;
import static org.folio.dataexp.util.ErrorCode.ERROR_CONVERTING_LD_TO_BIBFRAME;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.LinkedDataResource;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.service.export.LocalStorageWriter;
import org.folio.dataexp.service.export.strategies.ExportStrategy;
import org.folio.dataexp.service.export.strategies.ExportStrategyStatistic;
import org.folio.dataexp.service.export.strategies.ExportedRecordsListener;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.util.S3FilePathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;

/**
 * Abstract base class for Linked Data export strategies, providing common logic
 * that all implementations can use while providing their own retrievel mechanism.
 */
@Log4j2
@Getter
public abstract class AbstractLinkedDataExportStrategy implements ExportStrategy {
  protected int exportIdsBatch;
  protected String exportTmpStorage;

  private ExportIdEntityRepository exportIdEntityRepository;
  private JobProfileEntityRepository jobProfileEntityRepository;
  private LinkedDataConverter linkedDataConverter;

  protected ErrorLogService errorLogService;

  @Value("#{ T(Integer).parseInt('${application.export-ids-batch}')}")
  protected void setExportIdsBatch(int exportIdsBatch) {
    this.exportIdsBatch = exportIdsBatch;
  }

  @Value("${application.export-tmp-storage}")
  protected void setExportTmpStorage(String exportTmpStorage) {
    this.exportTmpStorage = exportTmpStorage;
  }

  @Override
  public ExportStrategyStatistic saveOutputToLocalStorage(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportRequest exportRequest,
      ExportedRecordsListener exportedRecordsListener
  ) {
    var exportStatistic = new ExportStrategyStatistic(exportedRecordsListener);
    var localStorageWriter = createLocalStorageWriter(exportFilesEntity);
    processSlices(exportFilesEntity, exportStatistic, localStorageWriter);
    try {
      localStorageWriter.close();
    } catch (Exception e) {
      log.error(
          "saveOutputToLocalStorage: Error saving file {} to local storage for job execution {}",
          exportFilesEntity.getFileLocation(),
          exportFilesEntity.getJobExecutionId()
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

  @Override
  public void setStatusBaseExportStatistic(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic
  ) {
    if (exportStatistic.getFailed() == 0 && exportStatistic.getExported() > 0) {
      exportFilesEntity.setStatus(JobExecutionExportFilesStatus.COMPLETED);
    }
    if (exportStatistic.getFailed() > 0 && exportStatistic.getExported() > 0) {
      exportFilesEntity.setStatus(JobExecutionExportFilesStatus.COMPLETED_WITH_ERRORS);
    }
    if (exportStatistic.getFailed() >= 0 && exportStatistic.getExported() == 0) {
      exportFilesEntity.setStatus(JobExecutionExportFilesStatus.FAILED);
      exportFilesEntity.setStatus(JobExecutionExportFilesStatus.FAILED);
    }
  }

  abstract List<LinkedDataResource> getLinkedDataResources(Set<UUID> externalIds);

  /**
   * Create a writer for saving an export job's output.
   *
   * @param exportFilesEntity export job
   * @return output writer
   */
  protected LocalStorageWriter createLocalStorageWriter(
      JobExecutionExportFilesEntity exportFilesEntity
  ) {
    return new LocalStorageWriter(
        S3FilePathUtils.getLocalStorageWriterPath(
            exportTmpStorage,
            exportFilesEntity.getFileLocation()
        ),
        OUTPUT_BUFFER_SIZE
    );
  }

  /**
   * Process the whole set of export IDs in slices, where each slice is turned into a
   * set and the real work of retrieving, converting, and writing is done for each set.
   * Note that the analogous MARC-generating processing tracks duplication errors, but
   * due to the way the only Linked Data implementation (so far) works, the input will
   * always be a set of UUIDs, and the retrieval mechanism should never respond with
   * more than one matching resource, so duplicate errors aren't tracked here.
   *
   * @param exportFilesEntity export job
   * @param exportStatistic export job statistics collector
   * @param localStorageWriter output writer
   */
  protected void processSlices(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic,
      LocalStorageWriter localStorageWriter
  ) {
    var slice = exportIdEntityRepository.getExportIds(
        exportFilesEntity.getJobExecutionId(),
        exportFilesEntity.getFromId(),
        exportFilesEntity.getToId(),
        PageRequest.of(0, exportIdsBatch)
    );
    log.info("Slice size: {}", slice.getSize());
    var exportIds = slice.getContent().stream()
        .map(ExportIdEntity::getInstanceId)
        .collect(Collectors.toSet());
    createAndSaveLinkedData(
        exportIds,
        exportStatistic,
        exportFilesEntity.getJobExecutionId(),
        localStorageWriter
    );
    while (slice.hasNext()) {
      slice = exportIdEntityRepository.getExportIds(
          exportFilesEntity.getJobExecutionId(),
          exportFilesEntity.getFromId(),
          exportFilesEntity.getToId(),
          slice.nextPageable()
      );
      exportIds = slice.getContent().stream()
          .map(ExportIdEntity::getInstanceId)
          .collect(Collectors.toSet());
      createAndSaveLinkedData(
          exportIds,
          exportStatistic,
          exportFilesEntity.getJobExecutionId(),
          localStorageWriter
      );
    }
  }

  /**
   * Retrieve, convert, and write Linked Data resources.
   *
   * @param externalIds set of instance identifiers to retrieve
   * @param exportStatistic export job statistics collector
   * @param jobExecutionId export job identifier
   * @param localStorageWriter output writer
   */
  protected void createAndSaveLinkedData(
      Set<UUID> externalIds,
      ExportStrategyStatistic exportStatistic,
      UUID jobExecutionId,
      LocalStorageWriter localStorageWriter
  ) {
    var resources = getLinkedDataResources(externalIds);
    log.info("linkedDataResources size: {}", resources.size());
    for (var resource : resources) {
      var os = StringUtils.EMPTY;
      try {
        os = linkedDataConverter.convertLdJsonToBibframe2Rdf(resource.getResource()).toString();
      } catch (Exception e) {
        exportStatistic.incrementFailed();
        saveConvertLinkedDataResourceError(resource, jobExecutionId, e);
        continue;
      }
      localStorageWriter.write(os);
      exportStatistic.incrementExported();
    }
    if (resources.size() < externalIds.size()) {
      var resultUuids = resources.stream()
          .map(LinkedDataResource::getInventoryId)
          .map(UUID::fromString)
          .collect(Collectors.toSet());
      externalIds.removeAll(resultUuids);
      exportStatistic.addNotExistIdsAll(externalIds.stream().toList());
    }
  }

  private void saveConvertLinkedDataResourceError(
      LinkedDataResource resource,
      UUID jobExecutionId,
      Exception e
  ) {
    var errorMessage = String.format(
        ERROR_CONVERTING_LD_TO_BIBFRAME.getDescription(),
        resource.getInventoryId()
    );
    log.error("{} : {}", errorMessage, e.getMessage());
    errorLogService.saveGeneralError(errorMessage, jobExecutionId);
  }

  @Autowired
  private void setExportIdEntityRepository(ExportIdEntityRepository exportIdEntityRepository) {
    this.exportIdEntityRepository = exportIdEntityRepository;
  }

  @Autowired
  private void setLinkedDataConverter(LinkedDataConverter linkedDataConverter) {
    this.linkedDataConverter = linkedDataConverter;
  }

  @Autowired
  private void setJobProfileEntityRepository(JobProfileEntityRepository
      jobProfileEntityRepository) {
    this.jobProfileEntityRepository = jobProfileEntityRepository;
  }

  @Autowired
  protected void setErrorLogService(ErrorLogService errorLogService) {
    this.errorLogService = errorLogService;
  }
}
