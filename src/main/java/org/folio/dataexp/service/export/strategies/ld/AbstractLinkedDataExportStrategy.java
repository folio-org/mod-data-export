package org.folio.dataexp.service.export.strategies.ld;

import static org.folio.dataexp.util.ErrorCode.ERROR_CONVERTING_LD_TO_BIBFRAME;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.LinkedDataResource;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.service.export.LocalStorageWriter;
import org.folio.dataexp.service.export.strategies.AbstractExportStrategy;
import org.folio.dataexp.service.export.strategies.ExportSliceResult;
import org.folio.dataexp.service.export.strategies.ExportStrategyStatistic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

/**
 * Abstract base class for Linked Data export strategies, providing common logic
 * that all implementations can use while providing their own retrievel mechanism.
 */
@Log4j2
@Getter
public abstract class AbstractLinkedDataExportStrategy extends AbstractExportStrategy {

  private static final String LD_SUFFIX = "json";
  protected int processSlicesThreadPoolSize;

  private LinkedDataConverter linkedDataConverter;

  @Override
  public String getFilenameSuffix() {
    return LD_SUFFIX;
  }

  abstract List<LinkedDataResource> getLinkedDataResources(Set<UUID> externalIds);

  @Value("#{T(Integer).parseInt('${application.process-slices-thread-pool-size}')}")
  protected void setProcessSlicesThreadPoolSize(int processSlicesThreadPoolSize) {
    this.processSlicesThreadPoolSize = processSlicesThreadPoolSize;
  }

  /**
   * Processes slices of export IDs for the export file entity. This implementation
   * takes a multithreaded approach. It is generic enough that it can be moved to the
   * superclass along with createAndSaveSliceRecords, replacing similar single-threaded
   * implementations in other strategies when determined to be appropriate.
   */
  @Override
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
    // Due to uses of the synchronized keyword in some of the methods called by
    // the per-thread work, virtual threads must be skipped, because synchronized
    // blocks cause virtual threads to be pinned to platform threads. This may
    // lead to resource exhaustion since virtual threads are treated as an
    // unlimited resource, but platform threads are not. With Java 24+,
    // synchronized blocks can be used with virtual threads, and this implementation
    // can be rewritten to use virtual threads without CompleteableFutures.
    try (var executor = Executors.newFixedThreadPool(processSlicesThreadPoolSize)) {
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
              executor)
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
   * return object setup. This is generic enough to be part of the abstract export
   * strategy once multithreading is considered to be mature enough for general use.
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
    var sliceStatistic = new ExportStrategyStatistic(exportStatistic.getExportedRecordsListener());
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
    return new ExportSliceResult(writer.getPath(), writer.getReader(), sliceStatistic);
  }

  /**
   * Consolidate slice results into a final output file.
   */
  private void copySliceResultToFinal(
      ExportSliceResult sliceResult,
      LocalStorageWriter finalOutput,
      UUID jobExecutionId
  ) {
    try {
      if (sliceResult.getStatistic().getExported() > 0) {
        var readerOpt = sliceResult.getReader();
        if (readerOpt.isPresent()) {
          var reader = readerOpt.get();
          String line;
          while ((line = reader.readLine()) != null) {
            finalOutput.write(line);
          }
          Files.deleteIfExists(sliceResult.getOutputFile());
        } else {
          sliceResult.getStatistic().failAll();
        }
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
   * Process the whole set of export IDs in slices, where each slice is turned into a
   * set and the real work of retrieving, converting, and writing is done for each set.
   * Note that the analogous MARC-generating processing tracks duplication errors, but
   * due to the way the only Linked Data implementation (so far) works, the input will
   * always be a set of UUIDs, and the retrieval mechanism should never respond with
   * more than one matching resource, so duplicate errors aren't tracked here.
   */
  @Override
  protected void createAndSaveRecords(
      Set<UUID> externalIds,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      UUID jobExecutionId,
      ExportRequest exportRequest,
      LocalStorageWriter writer
  ) {
    createAndSaveLinkedData(externalIds, exportStatistic, jobExecutionId, writer);
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
    for (var resource : resources) {
      var os = StringUtils.EMPTY;
      try {
        os = linkedDataConverter.convertLdJsonToBibframe2Rdf(resource.getResource()).toString();
      } catch (Exception e) {
        exportStatistic.incrementFailed();
        saveConvertLinkedDataResourceError(resource, jobExecutionId, e);
        continue;
      }
      localStorageWriter.write(os + System.lineSeparator());
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

  /**
   * Add a conversion error to this job's execution record.
   */
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
  private void setLinkedDataConverter(LinkedDataConverter linkedDataConverter) {
    this.linkedDataConverter = linkedDataConverter;
  }
}
