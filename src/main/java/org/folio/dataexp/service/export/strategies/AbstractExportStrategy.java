package org.folio.dataexp.service.export.strategies;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.service.export.LocalStorageWriter;
import org.folio.dataexp.repository.MarcAuthorityRecordAllRepository;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.util.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
  private JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  private ExportIdEntityRepository exportIdEntityRepository;

  private MappingProfileEntityRepository mappingProfileEntityRepository;
  private JobProfileEntityRepository jobProfileEntityRepository;
  private JobExecutionEntityRepository jobExecutionEntityRepository;
  private JsonToMarcConverter jsonToMarcConverter;
  private ErrorLogService errorLogService;
  protected MarcAuthorityRecordAllRepository marcAuthorityRecordAllRepository;

  @PersistenceContext
  protected EntityManager entityManager;

  @Value("#{ T(Integer).parseInt('${application.export-ids-batch}')}")
  protected void setExportIdsBatch(int exportIdsBatch) {
    this.exportIdsBatch = exportIdsBatch;
  }

  @Override
  public ExportStrategyStatistic saveMarcToRemoteStorage(JobExecutionExportFilesEntity exportFilesEntity, ExportRequest exportRequest) {
    var exportStatistic = new ExportStrategyStatistic();
    var mappingProfile = getMappingProfile(exportFilesEntity.getJobExecutionId());
    var localStorageWriter = createLocalStorageWrite(exportFilesEntity);
    processSlices(exportFilesEntity, exportStatistic, mappingProfile, exportRequest, localStorageWriter);
    try {
      localStorageWriter.close();
      setStatusBaseExportStatistic(exportFilesEntity, exportStatistic);
      jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
    } catch (Exception e) {
      log.error("saveMarcToRemoteStorage:: Error while saving file {} to local storage for job execution {}", exportFilesEntity.getFileLocation(), exportFilesEntity.getJobExecutionId());
      exportStatistic.setDuplicatedSrs(0);
      exportStatistic.setExported(0);
      long countFailed = exportIdEntityRepository.countExportIds(exportFilesEntity.getJobExecutionId(),
        exportFilesEntity.getFromId(), exportFilesEntity.getToId());
      exportStatistic.setFailed((int) countFailed);
      setStatusBaseExportStatistic(exportFilesEntity, exportStatistic);
      jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
    }
    return exportStatistic;
  }

  protected void setStatusBaseExportStatistic(JobExecutionExportFilesEntity exportFilesEntity, ExportStrategyStatistic exportStatistic) {
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

  abstract List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds, MappingProfile mappingProfile, ExportRequest exportRequest);

  abstract GeneratedMarcResult getGeneratedMarc(Set<UUID> ids, MappingProfile mappingProfile, ExportRequest exportRequest,
                                                UUID jobExecutionId, ExportStrategyStatistic exportStatistic);

  abstract Optional<ExportIdentifiersForDuplicateErrors> getIdentifiers(UUID id);

  abstract Map<UUID, MarcFields> getAdditionalMarcFieldsByExternalId(List<MarcRecordEntity> marcRecords, MappingProfile mappingProfile);

  protected LocalStorageWriter createLocalStorageWrite(JobExecutionExportFilesEntity exportFilesEntity) {
    return new LocalStorageWriter(exportFilesEntity.getFileLocation(), OUTPUT_BUFFER_SIZE);
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

  protected void createAndSaveMarc(Set<UUID> externalIds, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      UUID jobExecutionId, ExportRequest exportRequest, LocalStorageWriter localStorageWriter) {
    var externalIdsWithMarcRecord = new HashSet<UUID>();
    var marcRecords = getMarcRecords(externalIds, mappingProfile, exportRequest);
    createMarc(externalIds, exportStatistic, mappingProfile, jobExecutionId, externalIdsWithMarcRecord, marcRecords, localStorageWriter);
    var result = getGeneratedMarc(externalIds, mappingProfile, exportRequest, jobExecutionId, exportStatistic);
    saveMarc(result, exportStatistic, localStorageWriter);
  }

  protected void createMarc(Set<UUID> externalIds, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile,
      UUID jobExecutionId, Set<UUID> externalIdsWithMarcRecord, List<MarcRecordEntity> marcRecords, LocalStorageWriter localStorageWriter) {
    marcRecords = new ArrayList<>(marcRecords);
    log.info("marcRecords size: {}", marcRecords.size());
    var additionalFieldsPerId = getAdditionalMarcFieldsByExternalId(marcRecords, mappingProfile);
    var duplicatedUuidWithIdentifiers = new LinkedHashMap<UUID, Optional<ExportIdentifiersForDuplicateErrors>>();
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
      localStorageWriter.write(marc);
      if (externalIdsWithMarcRecord.contains(marcRecordEntity.getExternalId())) {
        exportStatistic.incrementDuplicatedSrs();
        var exportIdentifiers = getIdentifiers(marcRecordEntity.getExternalId());
        duplicatedUuidWithIdentifiers.put(marcRecordEntity.getExternalId(), exportIdentifiers);
      } else {
        externalIdsWithMarcRecord.add(marcRecordEntity.getExternalId());
      }
      exportStatistic.incrementExported();
    }
    saveDuplicateErrors(duplicatedUuidWithIdentifiers, marcRecords, jobExecutionId);
    marcRecords.clear();
    externalIds.removeAll(externalIdsWithMarcRecord);
  }

  protected void saveMarc(GeneratedMarcResult result, ExportStrategyStatistic exportStatistic, LocalStorageWriter localStorageWriter) {
    log.info("Generated marc size: {}", result.getMarcRecords().size());
    result.getMarcRecords().forEach(marc -> {
          if (StringUtils.isNotEmpty(marc)) {
            localStorageWriter.write(marc);
          }
          exportStatistic.incrementExported();
        });
    exportStatistic.setFailed(exportStatistic.getFailed() + result.getFailedIds().size());
    exportStatistic.addNotExistIdsAll(result.getNotExistIds());
  }

  private void saveDuplicateErrors(LinkedHashMap<UUID, Optional<ExportIdentifiersForDuplicateErrors>> duplicatedUuidWithIdentifiers,
      List<MarcRecordEntity> marcRecords, UUID jobExecutionId) {
    var externalIdsAsKeys = duplicatedUuidWithIdentifiers.keySet();
    for (var externalId : externalIdsAsKeys) {
      var exportIdentifiersOpt = duplicatedUuidWithIdentifiers.get(externalId);
      if (exportIdentifiersOpt.isPresent()) {
        var exportIdentifiers = exportIdentifiersOpt.get();
        var errorMessage = getDuplicatedSRSErrorMessage(externalId, marcRecords, exportIdentifiers);
        log.warn(errorMessage);
        if (exportIdentifiers.getAssociatedJsonObject() != null) {
          errorLogService.saveWithAffectedRecord(exportIdentifiers.getAssociatedJsonObject(), errorMessage, ErrorCode.ERROR_DUPLICATE_SRS_RECORD.getCode(), jobExecutionId);
        } else {
          errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_DUPLICATE_SRS_RECORD.getCode(), List.of(errorMessage), jobExecutionId);
        }
      }
    }
  }

  private String getDuplicatedSRSErrorMessage(UUID externalId, List<MarcRecordEntity> marcRecords, ExportIdentifiersForDuplicateErrors exportIdentifiers) {
    var marcRecordIds = marcRecords.stream().filter(m -> m.getExternalId().equals(externalId))
        .map(e -> e.getId().toString()).collect(Collectors.joining(", "));
    return String.format(ErrorCode.ERROR_DUPLICATE_SRS_RECORD.getDescription(), exportIdentifiers.getIdentifierHridMessage(), marcRecordIds);
  }

  private MappingProfile getMappingProfile(UUID jobExecutionId) {
    var jobExecution = jobExecutionEntityRepository.getReferenceById(jobExecutionId);
    var jobProfile = jobProfileEntityRepository.getReferenceById(jobExecution.getJobProfileId());
    return mappingProfileEntityRepository.getReferenceById(jobProfile.getMappingProfileId()).getMappingProfile();
  }

  protected void processSlices(JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile, ExportRequest exportRequest, LocalStorageWriter localStorageWriter) {
    var slice = exportIdEntityRepository.getExportIds(exportFilesEntity.getJobExecutionId(),
        exportFilesEntity.getFromId(), exportFilesEntity.getToId(), PageRequest.of(0, exportIdsBatch));
    log.info("Slice size: {}", slice.getSize());
    var exportIds = slice.getContent().stream().map(ExportIdEntity::getInstanceId).collect(Collectors.toSet());
    log.info("Size of exportIds: {}", exportIds.size());
    createAndSaveMarc(exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(), exportRequest, localStorageWriter);
    while (slice.hasNext()) {
      slice = exportIdEntityRepository.getExportIds(exportFilesEntity.getJobExecutionId(),
          exportFilesEntity.getFromId(), exportFilesEntity.getToId(), slice.nextPageable());
      exportIds = slice.getContent().stream().map(ExportIdEntity::getInstanceId).collect(Collectors.toSet());
      createAndSaveMarc(exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(),
          exportRequest, localStorageWriter);
    }
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
  private void setErrorLogService(ErrorLogService errorLogService) {
    this.errorLogService = errorLogService;
  }

  @Autowired
  private void setMarcAuthorityRecordAllRepository(MarcAuthorityRecordAllRepository marcAuthorityRecordAllRepository) {
    this.marcAuthorityRecordAllRepository = marcAuthorityRecordAllRepository;
  }
}
