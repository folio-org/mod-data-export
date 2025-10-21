package org.folio.dataexp.service.export.strategies;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;
import static net.minidev.json.parser.JSONParser.DEFAULT_PERMISSIVE_MODE;
import static org.folio.dataexp.util.ErrorCode.ERROR_CONVERTING_JSON_TO_MARC;
import static org.folio.dataexp.util.ErrorCode.ERROR_FIELDS_MAPPING_SRS;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.exception.TransformationRuleException;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.MarcAuthorityRecordAllRepository;
import org.folio.dataexp.service.export.LocalStorageWriter;
import org.folio.dataexp.util.ErrorCode;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/**
 * Abstract base class for MARC export strategies, providing common logic for exporting
 * MARC records.
 */
@Log4j2
@Getter
public abstract class AbstractMarcExportStrategy extends AbstractExportStrategy {

  private InstanceEntityRepository instanceEntityRepository;
  private JsonToMarcConverter jsonToMarcConverter;

  protected MarcAuthorityRecordAllRepository marcAuthorityRecordAllRepository;
  protected FolioExecutionContext folioExecutionContext;

  @PersistenceContext
  protected EntityManager entityManager;

  /**
   * Converts a JSON string to a JSONObject.
   *
   * @param jsonAsString the JSON string
   * @return Optional containing the parsed JSONObject, or empty if parsing fails
   */
  public static Optional<JSONObject> getAsJsonObject(String jsonAsString) {
    try {
      var jsonParser = new JSONParser(DEFAULT_PERMISSIVE_MODE);
      return Optional.of((JSONObject) jsonParser.parse(jsonAsString));
    } catch (ParseException e) {
      log.error("getAsJsonObject:: Error converting string to json {}", e.getMessage());
    }
    return Optional.empty();
  }

  /**
   * Gets MARC records for the given external IDs and mapping profile.
   */
  abstract List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds,
      MappingProfile mappingProfile, ExportRequest exportRequest, UUID jobExecutionId);

  /**
   * Gets a single MARC record by external ID.
   */
  public abstract MarcRecordEntity getMarcRecord(UUID externalId);

  /**
   * Gets the default mapping profile for the strategy.
   */
  public abstract MappingProfile getDefaultMappingProfile();

  /**
   * Generates MARC records for the given IDs and mapping profile.
   */
  abstract GeneratedMarcResult getGeneratedMarc(Set<UUID> ids, MappingProfile mappingProfile,
      ExportRequest exportRequest, UUID jobExecutionId, ExportStrategyStatistic exportStatistic);

  /**
   * Gets identifiers for duplicate error reporting.
   */
  abstract Optional<ExportIdentifiersForDuplicateError> getIdentifiers(UUID id);

  /**
   * Gets additional MARC fields by external ID.
   */
  abstract Map<UUID, MarcFields> getAdditionalMarcFieldsByExternalId(List<MarcRecordEntity>
      marcRecords, MappingProfile mappingProfile, UUID jobExecutionId)
      throws TransformationRuleException;

  /**
   * Creates and saves MARC records for the given external IDs.
   */
  protected void createAndSaveMarc(
      Set<UUID> externalIds,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      UUID jobExecutionId,
      ExportRequest exportRequest,
      LocalStorageWriter localStorageWriter
  ) {
    var externalIdsWithMarcRecord = new HashSet<UUID>();
    var marcRecords = getMarcRecords(externalIds, mappingProfile, exportRequest, jobExecutionId);
    createAndSaveMarcFromJsonRecord(
        externalIds, exportStatistic, mappingProfile, jobExecutionId, externalIdsWithMarcRecord,
        marcRecords, localStorageWriter
    );
    var result = getGeneratedMarc(
        externalIds, mappingProfile, exportRequest, jobExecutionId, exportStatistic
    );
    createAndSaveGeneratedMarc(result, exportStatistic, localStorageWriter);
  }

  /**
   * Creates and saves MARC records from JSON records.
   */
  protected void createAndSaveMarcFromJsonRecord(
      Set<UUID> externalIds,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      UUID jobExecutionId,
      Set<UUID> externalIdsWithMarcRecord,
      List<MarcRecordEntity> marcRecords,
      LocalStorageWriter localStorageWriter
  ) {
    marcRecords = new ArrayList<>(marcRecords);
    log.info("marcRecords size: {}", marcRecords.size());
    Map<UUID, MarcFields> additionalFieldsPerId;
    try {
      additionalFieldsPerId = getAdditionalMarcFieldsByExternalId(
          marcRecords, mappingProfile, jobExecutionId
      );
    } catch (TransformationRuleException e) {
      log.error(e);
      errorLogService.saveGeneralError(e.getMessage(), jobExecutionId);
      return;
    }
    var duplicatedUuidWithIdentifiers =
        new LinkedHashMap<UUID, Optional<ExportIdentifiersForDuplicateError>>();
    for (var marcRecordEntity : marcRecords) {
      var marc = StringUtils.EMPTY;
      try {
        var marcHoldingsItemsFields = additionalFieldsPerId.getOrDefault(
            marcRecordEntity.getExternalId(), new MarcFields()
        );
        if (!marcHoldingsItemsFields.getErrorMessages().isEmpty()) {
          errorLogService.saveGeneralErrorWithMessageValues(
              ERROR_FIELDS_MAPPING_SRS.getCode(),
              marcHoldingsItemsFields.getErrorMessages(),
              jobExecutionId
          );
        }
        marc = jsonToMarcConverter.convertJsonRecordToMarcRecord(
            marcRecordEntity.getContent(),
            marcHoldingsItemsFields.getHoldingItemsFields(),
            mappingProfile
        );
      } catch (Exception e) {
        exportStatistic.incrementFailed();
        saveConvertJsonRecordToMarcRecordError(marcRecordEntity, jobExecutionId, e);
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

  /**
   * Creates and saves generated MARC records.
   */
  protected void createAndSaveGeneratedMarc(
      GeneratedMarcResult result,
      ExportStrategyStatistic exportStatistic,
      LocalStorageWriter localStorageWriter
  ) {
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

  /**
   * Checks if the job profile is for deleted records.
   */
  protected boolean isDeletedJobProfile(UUID jobProfileId) {
    return StringUtils.equals(jobProfileId.toString(), "2c9be114-6d35-4408-adac-9ead35f51a27");
  }

  /**
   * Saves duplicate errors for MARC records.
   */
  private void saveDuplicateErrors(
      LinkedHashMap<UUID, Optional<ExportIdentifiersForDuplicateError>>
          duplicatedUuidIdentifiersMap,
      List<MarcRecordEntity> marcRecords,
      UUID jobExecutionId
  ) {
    var duplicatedUuids = duplicatedUuidIdentifiersMap.keySet();
    var srsIdByExternalId = getSrsIdByExternalIdMap(marcRecords);
    for (var externalId : duplicatedUuids) {
      var duplicatedIdentifiersOptional = duplicatedUuidIdentifiersMap.get(externalId);
      if (duplicatedIdentifiersOptional.isPresent()) {
        var duplicatedIdentifiers = duplicatedIdentifiersOptional.get();
        var errorMessage = getDuplicatedSrsErrorMessage(
            externalId, marcRecords, duplicatedIdentifiers
        );
        log.warn(errorMessage);
        var associatedJson = duplicatedIdentifiers.getAssociatedJsonObject();
        if (nonNull(associatedJson)) {
          errorLogService.saveWithAffectedRecord(
              associatedJson, errorMessage,
              ErrorCode.ERROR_DUPLICATE_SRS_RECORD.getCode(), jobExecutionId
          );
        } else {
          if (instanceEntityRepository.findByIdIn(Set.of(externalId)).isEmpty()) {
            errorLogService.saveGeneralErrorWithMessageValues(
                ErrorCode.ERROR_NON_EXISTING_INSTANCE.getCode(),
                List.of(
                    String.format(
                        ErrorCode.ERROR_NON_EXISTING_INSTANCE.getDescription(),
                        srsIdByExternalId.get(externalId)
                    )
                ),
                jobExecutionId
            );
          }
          errorLogService.saveGeneralErrorWithMessageValues(
              ErrorCode.ERROR_DUPLICATE_SRS_RECORD.getCode(),
              List.of(errorMessage),
              jobExecutionId
          );
        }
      }
    }
  }

  /**
   * Saves an error for converting JSON record to MARC record.
   */
  public void saveConvertJsonRecordToMarcRecordError(
      MarcRecordEntity marcRecordEntity,
      UUID jobExecutionId,
      Exception e
  ) {
    var errorMessage = String.format(
        ERROR_CONVERTING_JSON_TO_MARC.getDescription(),
        marcRecordEntity.getExternalId().toString()
    );
    log.error("{} : {}", errorMessage, e.getMessage());
    errorLogService.saveGeneralError(errorMessage, jobExecutionId);
  }

  /**
   * Gets the error message for duplicated SRS records.
   */
  private String getDuplicatedSrsErrorMessage(
      UUID externalId,
      List<MarcRecordEntity> marcRecords,
      ExportIdentifiersForDuplicateError exportIdentifiers
  ) {
    var marcRecordIds = marcRecords.stream()
        .filter(m -> m.getExternalId().equals(externalId))
        .map(e -> e.getId().toString())
        .collect(Collectors.joining(", "));
    return String.format(
        ErrorCode.ERROR_DUPLICATE_SRS_RECORD.getDescription(),
        exportIdentifiers.getIdentifierHridMessage(),
        marcRecordIds
    );
  }

  private Map<UUID, UUID> getSrsIdByExternalIdMap(List<MarcRecordEntity> marcRecords) {
    return marcRecords.stream()
        .collect(
            toMap(
                MarcRecordEntity::getExternalId,
                MarcRecordEntity::getId,
                (srsId1, srsId2) -> srsId1
            )
        );
  }

  @Override
  protected void createAndSaveRecords(
      Set<UUID> externalIds,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      UUID jobExecutionId,
      ExportRequest exportRequest,
      LocalStorageWriter writer
  ) {
    createAndSaveMarc(
        externalIds,
        exportStatistic,
        mappingProfile,
        jobExecutionId,
        exportRequest,
        writer
    );
  }

  @Override
  protected void processSlices(
      JobExecutionExportFilesEntity exportFilesEntity,
      ExportStrategyStatistic exportStatistic,
      MappingProfile mappingProfile,
      ExportRequest exportRequest,
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
    createAndSaveMarc(
        exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(),
        exportRequest, localStorageWriter
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
      createAndSaveMarc(
          exportIds, exportStatistic, mappingProfile, exportFilesEntity.getJobExecutionId(),
          exportRequest, localStorageWriter
      );
    }
  }

  @Autowired
  protected void setInstanceEntityRepository(InstanceEntityRepository instanceEntityRepository) {
    this.instanceEntityRepository = instanceEntityRepository;
  }

  @Autowired
  private void setJsonToMarcConverter(JsonToMarcConverter jsonToMarcConverter) {
    this.jsonToMarcConverter = jsonToMarcConverter;
  }

  @Autowired
  private void setMarcAuthorityRecordAllRepository(MarcAuthorityRecordAllRepository
      marcAuthorityRecordAllRepository) {
    this.marcAuthorityRecordAllRepository = marcAuthorityRecordAllRepository;
  }

  @Autowired
  private void setFolioExecutionContext(FolioExecutionContext folioExecutionContext) {
    this.folioExecutionContext = folioExecutionContext;
  }
}
