package org.folio.dataexp.service;

import static org.folio.dataexp.util.ErrorCode.ERROR_INVALID_CQL_SYNTAX;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.client.SearchClient;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.IdsJob;
import org.folio.dataexp.domain.dto.IdsJobPayload;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.exception.export.DataExportException;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.util.S3FilePathUtils;
import org.folio.s3.client.FolioS3Client;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Log4j2
public class InputFileProcessor {

  private static final int BATCH_SIZE_TO_SAVE = 1000;

  private final ExportIdEntityRepository exportIdEntityRepository;
  private final FolioS3Client s3Client;
  private final SearchClient searchClient;
  private final ErrorLogService errorLogService;
  private final JobExecutionService jobExecutionService;
  private final InsertExportIdService insertExportIdService;

  public void readFile(FileDefinition fileDefinition, CommonExportStatistic commonExportStatistic, ExportRequest.IdTypeEnum idType) {
    try {
      if (fileDefinition.getUploadFormat() == FileDefinition.UploadFormatEnum.CQL) {
        readCqlFile(fileDefinition, idType);
      } else {
        readCsvFile(fileDefinition, commonExportStatistic);
      }
    } catch (Exception e) {
      throw new DataExportException(e.getMessage());
    }
  }

  private void readCsvFile(FileDefinition fileDefinition, CommonExportStatistic commonExportStatistic) {
    var jobExecution = jobExecutionService.getById(fileDefinition.getJobExecutionId());
    var progress = jobExecution.getProgress();
    var pathToRead = S3FilePathUtils.getPathToUploadedFiles(fileDefinition.getId(), fileDefinition.getFileName());

    try (InputStream is = s3Client.read(pathToRead); BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      progress.setTotalIdsToRead((int) reader.lines().count());
      jobExecutionService.save(jobExecution);
    } catch (Exception e) {
      commonExportStatistic.setFailedToReadInputFile(true);
      log.error("Failed to read for file definition {}", fileDefinition.getId(), e);
    }
    var batch = new ArrayList<ExportIdEntity>();
    var duplicatedIds = new HashSet<UUID>();
    var countOfRead = new AtomicInteger();
    try (InputStream is = s3Client.read(pathToRead); BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      reader.lines().forEach(id -> {
        countOfRead.incrementAndGet();
        commonExportStatistic.setFailedToReadInputFile(false);
        var instanceId = id.replace("\"", StringUtils.EMPTY);
        try {
          var entity = ExportIdEntity.builder().jobExecutionId(fileDefinition
            .getJobExecutionId()).instanceId(UUID.fromString(instanceId)).build();
          if (!duplicatedIds.contains(entity.getInstanceId())) {
            batch.add(entity);
            duplicatedIds.add(entity.getInstanceId());
          } else {
            commonExportStatistic.incrementDuplicatedUUID();
          }
        } catch (Exception e) {
          log.error("Error converting {} to uuid", id);
          commonExportStatistic.addToInvalidUUIDFormat(id);
        }
        if (batch.size() == BATCH_SIZE_TO_SAVE) {
          insertExportIdService.saveBatch(batch);
          progress.setReadIds(countOfRead.get());
          jobExecutionService.save(jobExecution);
          batch.clear();
          duplicatedIds.clear();
        }
      });
    } catch (Exception e) {
      commonExportStatistic.setFailedToReadInputFile(true);
      log.error("Failed to read for file definition {}", fileDefinition.getId(), e);
    }
    insertExportIdService.saveBatch(batch);
    progress.setReadIds(countOfRead.get());
    jobExecutionService.save(jobExecution);

    int totalExportsIds = (int) exportIdEntityRepository.countByJobExecutionId(jobExecution.getId());
    int duplicatedFromDb = countOfRead.get() - totalExportsIds - commonExportStatistic.getDuplicatedUUIDAmount() - commonExportStatistic.getInvalidUUIDFormat().size();
    commonExportStatistic.incrementDuplicatedUUID(duplicatedFromDb);
  }

  private void readCqlFile(FileDefinition fileDefinition, ExportRequest.IdTypeEnum idType) throws IOException {
    var pathToRead = S3FilePathUtils.getPathToUploadedFiles(fileDefinition.getId(), fileDefinition.getFileName());
    String cql;
    try (InputStream is = s3Client.read(pathToRead)) {
      cql = IOUtils.toString(is, StandardCharsets.UTF_8);
    }
    if (Objects.nonNull(cql)) {
      try {
        var idsJobPayload = new IdsJobPayload().withEntityType(IdsJobPayload.EntityType.valueOf(idType.name())).withQuery(cql);
        var idsJob = searchClient.submitIdsJob(idsJobPayload);
        if (idsJob.getStatus() == IdsJob.Status.COMPLETED) {
          var resourceIds = searchClient.getResourceIds(idsJob.getId().toString());
          log.info("CQL totalRecords: {} for file definition id: {}", resourceIds.getTotalRecords(), fileDefinition.getId());
          List<ExportIdEntity> entities = resourceIds.getIds()
            .stream().map(id -> new ExportIdEntity()
              .withJobExecutionId(fileDefinition.getJobExecutionId())
              .withInstanceId(id.getId())).toList();
          exportIdEntityRepository.saveAll(entities);
        } else if (idsJob.getStatus() == IdsJob.Status.ERROR) {
          log.error(ERROR_INVALID_CQL_SYNTAX.getDescription(), fileDefinition.getFileName());
          errorLogService.saveGeneralErrorWithMessageValues(ERROR_INVALID_CQL_SYNTAX.getCode(), Collections.singletonList(fileDefinition.getFileName()),
            fileDefinition.getJobExecutionId());
        } else {
          log.error("Unexpected IdsJob.Status from mod-search: {}, file definition id: {}", idsJob.getStatus(), fileDefinition.getId());
        }
      } catch (Exception exc) {
        log.error("Error occurred while CQL export: {}, file definition id: {}", exc.getMessage(), fileDefinition.getId());
      }
    }
  }
}
