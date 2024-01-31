package org.folio.dataexp.service.export.strategies;

import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.ErrorLog;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.repository.MarcAuthorityRecordAllRepository;
import org.folio.dataexp.service.export.ExportExecutor;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.util.ErrorCode;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.RemoteStorageWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.minidev.json.parser.JSONParser.DEFAULT_PERMISSIVE_MODE;
import static org.folio.dataexp.service.export.Constants.OUTPUT_BUFFER_SIZE;
import static org.folio.dataexp.util.ErrorCode.ERROR_FIELDS_MAPPING_SRS;

@Log4j2
public abstract class AbstractExportStrategy implements ExportStrategy {

  protected int exportIdsBatch;
  private FolioS3Client s3Client;
  private JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  private ExportIdEntityRepository exportIdEntityRepository;

  private MappingProfileEntityRepository mappingProfileEntityRepository;
  private JobProfileEntityRepository jobProfileEntityRepository;
  private JobExecutionEntityRepository jobExecutionEntityRepository;
  private JsonToMarcConverter jsonToMarcConverter;
  private ErrorLogService errorLogService;
  protected MarcAuthorityRecordAllRepository marcAuthorityRecordAllRepository;
  private RemoteStorageWriter remoteStorageWriter;

  @Value("#{ T(Integer).parseInt('${application.export-ids-batch}')}")
  protected void setExportIdsBatch(int exportIdsBatch) {
    this.exportIdsBatch = exportIdsBatch;
  }

  @Override
  public ExportStrategyStatistic saveMarcToRemoteStorage(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest,
      boolean lastExport) {
    var exportStatistic = new ExportStrategyStatistic();
    var mappingProfile = getMappingProfile(exportFilesEntity.getJobExecutionId());
    this.remoteStorageWriter = createRemoteStorageWrite(exportFilesEntity);
    processSlices(exportFilesEntity, exportStatistic, mappingProfile, exportRequest);
    try {
      remoteStorageWriter.close();
      exportFilesEntity.setStatusBaseExportStatistic(exportStatistic);
      jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
    } catch (Exception e) {
      log.error("saveMarcToRemoteStorage:: Error while uploading file {} to remote storage for job execution {}", exportFilesEntity.getFileLocation(), exportFilesEntity.getJobExecutionId());
      exportStatistic.setDuplicatedSrs(0);
      exportStatistic.setExported(0);
      long countFailed = exportIdEntityRepository.countExportIds(exportFilesEntity.getJobExecutionId(),
        exportFilesEntity.getFromId(), exportFilesEntity.getToId());
      exportStatistic.setFailed((int) countFailed);
      exportFilesEntity.setStatusBaseExportStatistic(exportStatistic);
      jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
    }
    return exportStatistic;
  }

  abstract List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds, MappingProfile mappingProfile, ExportRequest exportRequest);

  abstract GeneratedMarcResult getGeneratedMarc(Set<UUID> ids, MappingProfile mappingProfile, ExportRequest exportRequest,
                                                UUID jobExecutionId, ExportStrategyStatistic exportStatistic);

  abstract Optional<String> getIdentifierMessage(UUID id);

  abstract Map<UUID, MarcFields> getAdditionalMarcFieldsByExternalId(List<MarcRecordEntity> marcRecords, MappingProfile mappingProfile);

  protected RemoteStorageWriter createRemoteStorageWrite(JobExecutionExportFilesEntity exportFilesEntity) {
    return new RemoteStorageWriter(exportFilesEntity.getFileLocation(), OUTPUT_BUFFER_SIZE, s3Client);
  }

  protected Optional<JSONObject> getAsJsonObject(String jsonAsString) {
    try {
      var jsonParser = new JSONParser(DEFAULT_PERMISSIVE_MODE);
      return Optional.of((JSONObject) jsonParser.parse(jsonAsString));
    } catch (ParseException e) {
      log.error("getAsJsonObject:: Error converting string to json {}", e.getMessage());
    }
    return Optional.empty();
  }

  protected void createAndSaveMarc(Set<UUID> externalIds, ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile, UUID jobExecutionId, ExportRequest exportRequest) {
    var marcRecords = getMarcRecords(externalIds, mappingProfile, exportRequest);
    log.info("marcRecords size: {}", marcRecords.size());
    var additionalFieldsPerId = getAdditionalMarcFieldsByExternalId(marcRecords, mappingProfile);
    var externalIdsWithMarcRecord = new HashSet<UUID>();
    var duplicatedSrsMessage = new HashSet<String>();
    for (var marcRecordEntity : marcRecords) {
      var marc = StringUtils.EMPTY;
      try {
        var marcHoldingsItemsFields = additionalFieldsPerId.getOrDefault(marcRecordEntity.getExternalId(), new MarcFields());
        marc = jsonToMarcConverter.convertJsonRecordToMarcRecord(marcRecordEntity.getContent(), marcHoldingsItemsFields.getHoldingItemsFields());
        if (marcHoldingsItemsFields.getErrorMessages().size() > 0) {
          errorLogService
            .saveGeneralErrorWithMessageValues(ERROR_FIELDS_MAPPING_SRS.getCode(), marcHoldingsItemsFields.getErrorMessages(), jobExecutionId);
        }
      } catch (Exception e) {
        log.error("Error converting json to marc for record {}", marcRecordEntity.getExternalId());
        exportStatistic.incrementFailed();
        continue;
      }
      remoteStorageWriter.write(marc);
      if (externalIdsWithMarcRecord.contains(marcRecordEntity.getExternalId())) {
        exportStatistic.incrementDuplicatedSrs();
        duplicatedSrsMessage.add(getDuplicatedSRSErrorMessage(marcRecordEntity.getExternalId(), marcRecords));
      } else {
        externalIdsWithMarcRecord.add(marcRecordEntity.getExternalId());
      }
      exportStatistic.incrementExported();
    }
    marcRecords.clear();
    externalIds.removeAll(externalIdsWithMarcRecord);
    var result = getGeneratedMarc(externalIds, mappingProfile, exportRequest, jobExecutionId, exportStatistic);
    log.info("Generated marc size: {}", result.getMarcRecords().size());
    result.getMarcRecords().forEach(marc -> {
      remoteStorageWriter.write(marc);
      exportStatistic.incrementExported();
      }
    );
    exportStatistic.setFailed(exportStatistic.getFailed() + result.getFailedIds().size());
    exportStatistic.addNotExistIdsAll(result.getNotExistIds());
    duplicatedSrsMessage.forEach(log::warn);
    if (!duplicatedSrsMessage.isEmpty()) {
      var errorLog = new ErrorLog();
      errorLog.setId(UUID.randomUUID());
      errorLog.createdDate(new Date());
      errorLog.setJobExecutionId(jobExecutionId);
      var message = String.join("; ", duplicatedSrsMessage);
      errorLog.setErrorMessageValues(List.of(message));
      errorLog.setErrorMessageCode(ErrorCode.ERROR_DUPLICATE_SRS_RECORD.getCode());
      errorLogService.save(errorLog);
    }
  }

  private String getDuplicatedSRSErrorMessage(UUID externalId, List<MarcRecordEntity> marcRecords) {
    var hridMessage = getIdentifierMessage(externalId);
    var marcRecordIds = marcRecords.stream().filter(m -> m.getExternalId().equals(externalId))
      .map(e -> e.getId().toString()).collect(Collectors.joining(", "));
    return hridMessage.map(hrid -> String.format(ErrorCode.ERROR_DUPLICATE_SRS_RECORD.getDescription(), hrid, marcRecordIds)).orElse(StringUtils.EMPTY);
  }

  private MappingProfile getMappingProfile(UUID jobExecutionId) {
    var jobExecution = jobExecutionEntityRepository.getReferenceById(jobExecutionId);
    var jobProfile = jobProfileEntityRepository.getReferenceById(jobExecution.getJobProfileId());
    return mappingProfileEntityRepository.getReferenceById(jobProfile.getMappingProfileId()).getMappingProfile();
  }

  protected void processSlices(JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile, ExportRequest exportRequest) {
    var slice = exportIdEntityRepository.getExportIds(exportFilesEntity.getJobExecutionId(),
        exportFilesEntity.getFromId(), exportFilesEntity.getToId(), PageRequest.of(0, exportIdsBatch));
    log.info("Slice size: {}", slice.getSize());
    var exportIds = slice.getContent().stream().map(ExportIdEntity::getInstanceId).collect(Collectors.toSet());
    log.info("Size of exportIds: {}", exportIds.size());
    createAndSaveMarc(exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(), exportRequest);
    while (slice.hasNext()) {
      slice = exportIdEntityRepository.getExportIds(exportFilesEntity.getJobExecutionId(),
          exportFilesEntity.getFromId(), exportFilesEntity.getToId(), slice.nextPageable());
      exportIds = slice.getContent().stream().map(ExportIdEntity::getInstanceId).collect(Collectors.toSet());
      createAndSaveMarc(exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(),
          exportRequest);
    }
  }

  protected void updateSliceState(Slice<?> slice, ExportRequest exportRequest) {
    exportRequest.setLastSlice(slice.isLast());
  }

  protected boolean isExportCompleted(ExportRequest exportRequest) {
    return exportRequest.getLastSlice() && exportRequest.getLastExport();
  }

  @Autowired
  private void setJobExecutionExportFilesEntityRepository(JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository) {
    this.jobExecutionExportFilesEntityRepository = jobExecutionExportFilesEntityRepository;
  }

  @Autowired
  private void setExportIdEntityRepository(ExportIdEntityRepository exportIdEntityRepository) {
    this.exportIdEntityRepository = exportIdEntityRepository;
  }

  @Autowired
  private void setJsonToMarcConverter(JsonToMarcConverter jsonToMarcConverter) {
    this.jsonToMarcConverter = jsonToMarcConverter;
  }

  @Autowired
  private void setMappingProfileEntityRepository(MappingProfileEntityRepository mappingProfileEntityRepository) {
    this.mappingProfileEntityRepository = mappingProfileEntityRepository;
  }

  @Autowired
  private void setJobProfileEntityRepository(JobProfileEntityRepository jobProfileEntityRepository) {
    this.jobProfileEntityRepository = jobProfileEntityRepository;
  }

  @Autowired
  private void setJobExecutionEntityRepository(JobExecutionEntityRepository jobExecutionEntityRepository) {
    this.jobExecutionEntityRepository = jobExecutionEntityRepository;
  }

  @Autowired
  private void setS3Client(FolioS3Client s3Client) {
    this.s3Client = s3Client;
  }

  @Autowired
  private void setErrorLogService(ErrorLogService errorLogService) {
    this.errorLogService = errorLogService;
  }

  @Autowired
  private void setMarcAuthorityRecordAllRepository(MarcAuthorityRecordAllRepository marcAuthorityRecordAllRepository) {
    this.marcAuthorityRecordAllRepository = marcAuthorityRecordAllRepository;
  }
}
