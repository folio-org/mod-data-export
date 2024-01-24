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
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.repository.MarcAuthorityRecordAllRepository;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.util.ErrorCode;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.RemoteStorageWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

  private int exportIdsBatch;
  private FolioS3Client s3Client;
  private JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  private ExportIdEntityRepository exportIdEntityRepository;

  private MappingProfileEntityRepository mappingProfileEntityRepository;
  private JobProfileEntityRepository jobProfileEntityRepository;
  private JobExecutionEntityRepository jobExecutionEntityRepository;
  private JsonToMarcConverter jsonToMarcConverter;
  private ErrorLogService errorLogService;
  private HoldingsRecordEntityRepository holdingsRecordEntityRepository;
  private MarcAuthorityRecordAllRepository marcAuthorityRecordAllRepository;
  private InstanceEntityRepository instanceEntityRepository;

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
    if (Boolean.TRUE.equals(exportRequest.getAll())) {
      if (exportRequest.getIdType() == ExportRequest.IdTypeEnum.HOLDING) {
        processSlicesHoldingsAll(exportFilesEntity, exportStatistic, mappingProfile, exportRequest, lastExport);
      } else if (exportRequest.getIdType() == ExportRequest.IdTypeEnum.AUTHORITY) {
        processSlicesAuthoritiesAll(exportFilesEntity, exportStatistic, mappingProfile, exportRequest, lastExport);
      } else if (exportRequest.getIdType() == ExportRequest.IdTypeEnum.INSTANCE) {
        processSlicesInstancesAll(exportFilesEntity, exportStatistic, mappingProfile, exportRequest, lastExport);
      } else {
        throw new UnsupportedOperationException(exportRequest.getIdType() + " id type is not supported.");
      }
    } else {
      processSlices(exportFilesEntity, exportStatistic, mappingProfile, exportRequest, lastExport);
    }
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
                                                boolean lastSlice, boolean lastExport, UUID jobExecutionId, ExportStrategyStatistic exportStatistic);

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

  private void createAndSaveMarc(Set<UUID> externalIds, ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile, UUID jobExecutionId, ExportRequest exportRequest, boolean lastSlice, boolean lastExport) {
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
    var result = getGeneratedMarc(externalIds, mappingProfile, exportRequest, lastSlice, lastExport, jobExecutionId, exportStatistic);
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

  private void processSlices(JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile, ExportRequest exportRequest, boolean lastExport) {
    var slice = exportIdEntityRepository.getExportIds(exportFilesEntity.getJobExecutionId(),
        exportFilesEntity.getFromId(), exportFilesEntity.getToId(), PageRequest.of(0, exportIdsBatch));
    log.info("Slice size: {}", slice.getSize());
    var exportIds = slice.getContent().stream().map(ExportIdEntity::getInstanceId).collect(Collectors.toSet());
    log.info("Size of exportIds: {}", exportIds.size());
    createAndSaveMarc(exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(), exportRequest,
        slice.isLast(), lastExport);
    while (slice.hasNext()) {
      slice = exportIdEntityRepository.getExportIds(exportFilesEntity.getJobExecutionId(),
          exportFilesEntity.getFromId(), exportFilesEntity.getToId(), slice.nextPageable());
      exportIds = slice.getContent().stream().map(ExportIdEntity::getInstanceId).collect(Collectors.toSet());
      createAndSaveMarc(exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(),
          exportRequest, slice.isLast(), lastExport);
    }
  }

  private void processSlicesHoldingsAll(JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile, ExportRequest exportRequest, boolean lastExport) {
    var slice = chooseSliceHoldings(exportFilesEntity, exportRequest, PageRequest.of(0, exportIdsBatch));
    log.info("Slice size: {}", slice.getSize());
    var exportIds = slice.getContent().stream().map(HoldingsRecordEntity::getId).collect(Collectors.toSet());
    log.info("Size of exportIds: {}", exportIds.size());
    createAndSaveMarc(exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(),
        exportRequest, slice.isLast(), lastExport);
    while (slice.hasNext()) {
      slice = chooseSliceHoldings(exportFilesEntity, exportRequest, slice.nextPageable());
      exportIds = slice.getContent().stream().map(HoldingsRecordEntity::getId).collect(Collectors.toSet());
      createAndSaveMarc(exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(),
          exportRequest, slice.isLast(), lastExport);
    }
  }

  private void processSlicesAuthoritiesAll(JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile, ExportRequest exportRequest, boolean lastExport) {
    var slice = chooseSliceAuthorities(exportFilesEntity, exportRequest, PageRequest.of(0, exportIdsBatch));
    var exportIds = slice.getContent().stream().map(MarcRecordEntity::getExternalId).collect(Collectors.toSet());
    createAndSaveMarc(exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(),
        exportRequest, slice.isLast(), lastExport);
    while (slice.hasNext()) {
      slice = chooseSliceAuthorities(exportFilesEntity, exportRequest, slice.nextPageable());
      exportIds = slice.getContent().stream().map(MarcRecordEntity::getExternalId).collect(Collectors.toSet());
      createAndSaveMarc(exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(),
          exportRequest, slice.isLast(), lastExport);
    }
  }

  private void processSlicesInstancesAll(JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile, ExportRequest exportRequest, boolean lastExport) {
    var slice = chooseSliceInstances(exportFilesEntity, exportRequest, PageRequest.of(0, exportIdsBatch));
    var exportIds = slice.getContent().stream().map(InstanceEntity::getId).collect(Collectors.toSet());
    createAndSaveMarc(exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(),
        exportRequest, slice.isLast(), lastExport);
    while (slice.hasNext()) {
      slice = chooseSliceInstances(exportFilesEntity, exportRequest, slice.nextPageable());
      exportIds = slice.getContent().stream().map(InstanceEntity::getId).collect(Collectors.toSet());
      createAndSaveMarc(exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(),
          exportRequest, slice.isLast(), lastExport);
    }
  }

  private Slice<HoldingsRecordEntity> chooseSliceHoldings(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest, Pageable pageble) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return holdingsRecordEntityRepository.findByIdGreaterThanEqualAndIdLessThanEqualOrderByIdAsc(exportFilesEntity.getFromId(),
          exportFilesEntity.getToId(), pageble);
    }
    return holdingsRecordEntityRepository.findAllWhenSkipDiscoverySuppressed(exportFilesEntity.getFromId(),
        exportFilesEntity.getToId(), pageble);
  }

  private Slice<MarcRecordEntity> chooseSliceAuthorities(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest, Pageable pageble) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      if (Boolean.TRUE.equals(exportRequest.getDeletedRecords())) {
        return marcAuthorityRecordAllRepository.findAllWithDeleted(exportFilesEntity.getFromId(), exportFilesEntity.getToId(), pageble);
      }
      return marcAuthorityRecordAllRepository.findAllWithoutDeleted(exportFilesEntity.getFromId(), exportFilesEntity.getToId(), pageble);
    }
    if (Boolean.TRUE.equals(exportRequest.getDeletedRecords())) {
      return marcAuthorityRecordAllRepository.findAllWithDeletedWhenSkipDiscoverySuppressed(exportFilesEntity.getFromId(),
          exportFilesEntity.getToId(), pageble);
    }
    return marcAuthorityRecordAllRepository.findAllWithoutDeletedWhenSkipDiscoverySuppressed(exportFilesEntity.getFromId(),
        exportFilesEntity.getToId(), pageble);
  }

  private Slice<InstanceEntity> chooseSliceInstances(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest, Pageable pageble) {
    if (Boolean.TRUE.equals(exportRequest.getSuppressedFromDiscovery())) {
      return instanceEntityRepository.findByIdGreaterThanEqualAndIdLessThanEqualOrderByIdAsc(exportFilesEntity.getFromId(),
          exportFilesEntity.getToId(), pageble);
    }
    return instanceEntityRepository.findAllWhenSkipDiscoverySuppressed(exportFilesEntity.getFromId(),
        exportFilesEntity.getToId(), pageble);
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
  private void setHoldingsRecordEntityRepository(HoldingsRecordEntityRepository holdingsRecordEntityRepository) {
    this.holdingsRecordEntityRepository = holdingsRecordEntityRepository;
  }

  @Autowired
  private void setMarcAuthorityRecordAllRepository(MarcAuthorityRecordAllRepository marcAuthorityRecordAllRepository) {
    this.marcAuthorityRecordAllRepository = marcAuthorityRecordAllRepository;
  }

  @Autowired
  private void setInstanceEntityRepository(InstanceEntityRepository instanceEntityRepository) {
    this.instanceEntityRepository = instanceEntityRepository;
  }
}
