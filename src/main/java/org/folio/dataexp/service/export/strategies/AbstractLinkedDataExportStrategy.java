package org.folio.dataexp.service.export.strategies;

import static org.folio.dataexp.service.export.Constants.OUTPUT_BUFFER_SIZE;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;

@Log4j2
@Getter
public abstract class AbstractLinkedDataExportStrategy implements ExportStrategy {
  protected int exportIdsBatch;
  protected String exportTmpStorage;

  private ExportIdEntityRepository exportIdEntityRepository;
  private MappingProfileEntityRepository mappingProfileEntityRepository;
  private JobProfileEntityRepository jobProfileEntityRepository;
  private JobExecutionService jobExecutionService;
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
  public ExportStrategyStatistic saveOutputToLocalStorage(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest, ExportedMarcListener exportedMarcListener) {
    var exportStatistic = new ExportStrategyStatistic(exportedMarcListener);
    var mappingProfile = getMappingProfile(exportFilesEntity.getJobExecutionId());
    var localStorageWriter = createLocalStorageWrite(exportFilesEntity);
    processSlices(exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    try {
      localStorageWriter.close();
    } catch (Exception e) {
      log.error("saveLinkedDataToLocalStorage:: Error while saving file {} to local storage for job execution {}", exportFilesEntity.getFileLocation(), exportFilesEntity.getJobExecutionId());
      exportStatistic.setDuplicatedSrs(0);
      exportStatistic.removeExported();
      long countFailed = exportIdEntityRepository.countExportIds(exportFilesEntity.getJobExecutionId(),
        exportFilesEntity.getFromId(), exportFilesEntity.getToId());
      exportStatistic.setFailed((int) countFailed);
    }
    return exportStatistic;
  }

  @Override
  public void setStatusBaseExportStatistic(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic) {
    if (exportStatistic.getFailed() == 0 && exportStatistic.getExported() > 0) {
      exportFilesEntity.setStatus(JobExecutionExportFilesStatus.COMPLETED);
    }
    if (exportStatistic.getFailed() > 0 && exportStatistic.getExported() > 0) {
      exportFilesEntity.setStatus(JobExecutionExportFilesStatus.COMPLETED_WITH_ERRORS);
    }
    if (exportStatistic.getFailed() >= 0 && exportStatistic.getExported() == 0) {
      exportFilesEntity.setStatus(JobExecutionExportFilesStatus.FAILED);
    }
  }

  abstract List<String> getLinkedDataResources(Set<UUID> externalIds, MappingProfile mappingProfile, ExportRequest exportRequest, UUID jobExecutionId);

  protected LocalStorageWriter createLocalStorageWrite(JobExecutionExportFilesEntity exportFilesEntity) {
    return new LocalStorageWriter(S3FilePathUtils.getLocalStorageWriterPath(exportTmpStorage, exportFilesEntity.getFileLocation()), OUTPUT_BUFFER_SIZE);
  }

  private MappingProfile getMappingProfile(UUID jobExecutionId) {
    var jobExecution = jobExecutionService.getById(jobExecutionId);
    var jobProfile = jobProfileEntityRepository.getReferenceById(jobExecution.getJobProfileId());
    return mappingProfileEntityRepository.getReferenceById(jobProfile.getMappingProfileId()).getMappingProfile();
  }

  protected void processSlices(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile, ExportRequest exportRequest, LocalStorageWriter localStorageWriter) {
    var slice = exportIdEntityRepository.getExportIds(exportFilesEntity.getJobExecutionId(),
        exportFilesEntity.getFromId(), exportFilesEntity.getToId(), PageRequest.of(0, exportIdsBatch));
    log.info("Slice size: {}", slice.getSize());
    var exportIds = slice.getContent().stream().map(ExportIdEntity::getInstanceId).collect(Collectors.toSet());
    createAndSaveLinkedData(exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(), exportRequest, localStorageWriter);
    while (slice.hasNext()) {
      slice = exportIdEntityRepository.getExportIds(exportFilesEntity.getJobExecutionId(),
          exportFilesEntity.getFromId(), exportFilesEntity.getToId(), slice.nextPageable());
      exportIds = slice.getContent().stream().map(ExportIdEntity::getInstanceId).collect(Collectors.toSet());
      createAndSaveLinkedData(exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(),
          exportRequest, localStorageWriter);
    }
  }

  protected void createAndSaveLinkedData(Set<UUID> externalIds, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      UUID jobExecutionId, ExportRequest exportRequest, LocalStorageWriter localStorageWriter) {
    // TODO: placeholder, implement correctly
    var resources = getLinkedDataResources(externalIds, mappingProfile, exportRequest, jobExecutionId);
    for (var resource : resources) {
      var os = StringUtils.EMPTY;
      try {
        os = linkedDataConverter.convertLdJsonToBibframe2Rdf(resource).toString();
      } catch (Exception e) {
        exportStatistic.incrementFailed();
        // TODO: define, implement
        // saveConvertLinkedDataResourceError(resource, jobExecutionId, e);
        continue;
      }
      localStorageWriter.write(os);
      exportStatistic.incrementExported();
    }
  }

}
