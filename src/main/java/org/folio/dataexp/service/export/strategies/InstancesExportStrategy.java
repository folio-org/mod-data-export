package org.folio.dataexp.service.export.strategies;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.folio.dataexp.service.export.Constants.DEFAULT_INSTANCE_MAPPING_PROFILE_ID;
import static org.folio.dataexp.service.export.Constants.DELETED_KEY;
import static org.folio.dataexp.service.export.Constants.HRID_KEY;
import static org.folio.dataexp.service.export.Constants.ID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_KEY;
import static org.folio.dataexp.service.export.Constants.TITLE_KEY;
import static org.folio.dataexp.util.Constants.LEADER_STATUS_DELETED;
import static org.folio.dataexp.util.Constants.STATE_ACTUAL;
import static org.folio.dataexp.util.Constants.STATE_DELETED;
import static org.folio.dataexp.util.ErrorCode.ERROR_CONVERTING_TO_JSON_INSTANCE;
import static org.folio.dataexp.util.FolioExecutionContextUtil.prepareContextForTenant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.exception.TransformationRuleException;
import org.folio.dataexp.exception.export.DownloadRecordException;
import org.folio.dataexp.repository.InstanceCentralTenantRepository;
import org.folio.dataexp.repository.InstanceWithHridEntityRepository;
import org.folio.dataexp.repository.MarcInstanceRecordRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.dataexp.util.ErrorCode;
import org.folio.processor.RuleProcessor;
import org.folio.processor.referencedata.ReferenceDataWrapper;
import org.folio.processor.rule.Rule;
import org.folio.reader.EntityReader;
import org.folio.reader.JPathSyntaxEntityReader;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.writer.RecordWriter;
import org.folio.writer.impl.MarcRecordWriter;
import org.marc4j.MarcException;
import org.marc4j.marc.VariableField;
import org.springframework.stereotype.Component;

/** Export strategy for instances, handling MARC and generated records. */
@Log4j2
@Component
@AllArgsConstructor
public class InstancesExportStrategy extends AbstractMarcExportStrategy {

  protected static final String INSTANCE_MARC_TYPE = "MARC_BIB";
  protected static final String LONG_MARC_RECORD_MESSAGE =
      "Record is too long to be a valid MARC binary record";
  public static final String DEFAULT_INSTANCE_PROFILE_ID = "25d81cbe-9686-11ea-bb37-0242ac130002";

  private final ConsortiaService consortiaService;
  private final InstanceCentralTenantRepository instanceCentralTenantRepository;
  private final MarcInstanceRecordRepository marcInstanceRecordRepository;
  private final RuleFactory ruleFactory;
  private final RuleHandler ruleHandler;
  private final RuleProcessor ruleProcessor;
  private final ReferenceDataProvider referenceDataProvider;
  private final InstanceWithHridEntityRepository instanceWithHridEntityRepository;
  private final HoldingsItemsResolverService holdingsItemsResolver;
  private final FolioModuleMetadata folioModuleMetadata;

  protected final MarcRecordEntityRepository marcRecordEntityRepository;

  /**
   * Retrieves MARC records for the given external IDs and mapping profile.
   *
   * @param externalIds Set of external UUIDs.
   * @param mappingProfile The mapping profile.
   * @param exportRequest The export request.
   * @param jobExecutionId The job execution ID.
   * @return List of MarcRecordEntity.
   */
  @Override
  public List<MarcRecordEntity> getMarcRecords(
      Set<UUID> externalIds,
      MappingProfile mappingProfile,
      ExportRequest exportRequest,
      UUID jobExecutionId) {
    if (Boolean.TRUE.equals(mappingProfile.getDefault())
        || mappingProfile.getRecordTypes().contains(RecordTypes.SRS)) {
      var marcInstances =
          marcRecordEntityRepository.findByExternalIdInAndRecordTypeIsAndStateIn(
              externalIds, INSTANCE_MARC_TYPE, Set.of(STATE_ACTUAL, STATE_DELETED));
      processDeletedInstances(marcInstances);
      var foundIds =
          marcInstances.stream().map(MarcRecordEntity::getExternalId).collect(Collectors.toSet());
      externalIds.removeAll(foundIds);
      if (!externalIds.isEmpty()) {
        var centralTenantId =
            consortiaService.getCentralTenantId(folioExecutionContext.getTenantId());
        if (StringUtils.isNotEmpty(centralTenantId)) {
          var marcInstancesFromCentralTenant =
              marcInstanceRecordRepository.findByExternalIdIn(centralTenantId, externalIds);
          marcInstances.addAll(marcInstancesFromCentralTenant);
        }
      }
      return marcInstances;
    }
    return new ArrayList<>();
  }

  private void processDeletedInstances(List<MarcRecordEntity> marcInstances) {
    if (!consortiaService.isCurrentTenantCentralTenant(folioExecutionContext.getTenantId())) {
      var deletedInstanceIds =
          marcInstances.stream()
              .filter(this::isDeleted)
              .map(MarcRecordEntity::getId)
              .collect(Collectors.toSet());
      if (!deletedInstanceIds.isEmpty()) {
        var centralTenantId =
            consortiaService.getCentralTenantId(folioExecutionContext.getTenantId());
        try (var ignored =
            new FolioExecutionContextSetter(
                prepareContextForTenant(
                    centralTenantId, folioModuleMetadata, folioExecutionContext))) {
          var sharedInstances =
              marcRecordEntityRepository.findByExternalIdInAndRecordTypeIsAndStateIn(
                  deletedInstanceIds, INSTANCE_MARC_TYPE, Set.of(STATE_ACTUAL, STATE_DELETED));
          log.info("Found instance: {}", sharedInstances.getFirst());
          combineLists(marcInstances, sharedInstances);
        }
      }
    }
  }

  private boolean isDeleted(MarcRecordEntity entity) {
    return STATE_DELETED.equals(entity.getState())
        || (STATE_ACTUAL.equals(entity.getState())
            && LEADER_STATUS_DELETED == entity.getLeaderRecordStatus());
  }

  private void combineLists(List<MarcRecordEntity> dest, List<MarcRecordEntity> source) {
    if (!source.isEmpty()) {
      var lookup = source.stream()
          .collect(Collectors.toMap(MarcRecordEntity::getExternalId, e -> e));
      for (int i = 0; i < dest.size(); i++) {
        var replacement = lookup.get(dest.get(i).getExternalId());
        if (replacement != null) {
          dest.set(i, replacement);
        }
      }
    }
  }

  /**
   * Generates MARC records for the given instance IDs and mapping profile.
   *
   * @param instanceIds Set of instance UUIDs.
   * @param mappingProfile The mapping profile.
   * @param exportRequest The export request.
   * @param jobExecutionId The job execution ID.
   * @param exportStatistic The export statistic.
   * @return GeneratedMarcResult containing MARC records and statistics.
   */
  @Override
  public GeneratedMarcResult getGeneratedMarc(
      Set<UUID> instanceIds,
      MappingProfile mappingProfile,
      ExportRequest exportRequest,
      UUID jobExecutionId,
      ExportStrategyStatistic exportStatistic) {
    var generatedMarcResult = new GeneratedMarcResult(jobExecutionId);
    var instancesWithHoldingsAndItems =
        getInstancesWithHoldingsAndItems(instanceIds, generatedMarcResult, mappingProfile);
    return getGeneratedMarc(
        generatedMarcResult, instancesWithHoldingsAndItems, mappingProfile, jobExecutionId);
  }

  /**
   * Generates MARC records using the provided result, instances, and mapping profile.
   *
   * @param generatedMarcResult The result object to populate.
   * @param instancesWithHoldingsAndItems List of instance JSON objects.
   * @param mappingProfile The mapping profile.
   * @param jobExecutionId The job execution ID.
   * @return The populated GeneratedMarcResult.
   */
  protected GeneratedMarcResult getGeneratedMarc(
      GeneratedMarcResult generatedMarcResult,
      List<JSONObject> instancesWithHoldingsAndItems,
      MappingProfile mappingProfile,
      UUID jobExecutionId) {
    var marcRecords = new ArrayList<String>();
    ReferenceDataWrapper referenceData = getReferenceData();
    List<Rule> rules;
    try {
      rules = getRules(mappingProfile);
    } catch (TransformationRuleException e) {
      log.error(e);
      errorLogService.saveGeneralError(e.getMessage(), jobExecutionId);
      return generatedMarcResult;
    }
    for (var jsonObject : instancesWithHoldingsAndItems) {
      try {
        var marc = mapToMarc(jsonObject, rules, referenceData);
        marcRecords.add(marc);
      } catch (MarcException e) {
        var instanceJson = (JSONObject) jsonObject.get(INSTANCE_KEY);
        log.debug("getGeneratedMarc instanceJson: {}", instanceJson);
        var uuid = instanceJson.getAsString(ID_KEY);
        generatedMarcResult.addIdToFailed(UUID.fromString(uuid));
        errorLogService.saveWithAffectedRecord(
            instanceJson,
            ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode(),
            jobExecutionId,
            e);
        log.error(
            " getGeneratedMarc:: exception to convert in marc : {} for instance {}",
            e.getMessage(),
            uuid);
        if (instanceJson.containsKey(DELETED_KEY) && (boolean) instanceJson.get(DELETED_KEY)) {
          errorLogService.saveGeneralErrorWithMessageValues(
              ErrorCode.ERROR_DELETED_TOO_LONG_INSTANCE.getCode(), List.of(uuid), jobExecutionId);
          log.error(
              String.format(ErrorCode.ERROR_DELETED_TOO_LONG_INSTANCE.getDescription(), uuid));
        }
      }
    }
    generatedMarcResult.setMarcRecords(marcRecords);
    return generatedMarcResult;
  }

  /**
   * Retrieves identifiers for duplicate error reporting.
   *
   * @param id The UUID of the instance.
   * @return Optional containing ExportIdentifiersForDuplicateError if found.
   */
  @Override
  public Optional<ExportIdentifiersForDuplicateError> getIdentifiers(UUID id) {
    var instances = instanceEntityRepository.findByIdIn(Set.of(id));
    if (instances.isEmpty()) {
      log.info("getIdentifiers:: not found for instance by id {}", id);
      return getDefaultIdentifiers(id);
    }
    var jsonObject = getAsJsonObject(instances.get(0).getJsonb());
    if (jsonObject.isPresent()) {
      var uuid = jsonObject.get().getAsString(ID_KEY);
      var exportIdentifiers = new ExportIdentifiersForDuplicateError();
      var hrid = jsonObject.get().getAsString(HRID_KEY);
      exportIdentifiers.setIdentifierHridMessage("Instance with HRID: " + hrid);
      var instanceAssociatedJsonObject = new JSONObject();
      instanceAssociatedJsonObject.put(ErrorLogService.ID, uuid);
      instanceAssociatedJsonObject.put(ErrorLogService.HRID, hrid);
      var title = jsonObject.get().getAsString(TITLE_KEY);
      instanceAssociatedJsonObject.put(ErrorLogService.TITLE, title);
      exportIdentifiers.setAssociatedJsonObject(instanceAssociatedJsonObject);
      return Optional.of(exportIdentifiers);
    }
    return getDefaultIdentifiers(id);
  }

  /**
   * Saves an error when converting a JSON record to a MARC record fails.
   *
   * @param marcRecordEntity The MARC record entity.
   * @param jobExecutionId The job execution ID.
   * @param e The exception thrown.
   */
  @Override
  public void saveConvertJsonRecordToMarcRecordError(
      MarcRecordEntity marcRecordEntity, UUID jobExecutionId, Exception e) {
    var errorMessage = e.getMessage();
    var instances = instanceEntityRepository.findByIdIn(Set.of(marcRecordEntity.getExternalId()));
    if (errorMessage.contains(LONG_MARC_RECORD_MESSAGE) && !instances.isEmpty()) {
      var jsonObject = getAsJsonObject(instances.get(0).getJsonb());
      if (jsonObject.isPresent()) {
        var instanceJson = jsonObject.get();
        instanceJson.put(DELETED_KEY, instances.get(0).isDeleted());
        errorLogService.saveWithAffectedRecord(
            instanceJson,
            e.getMessage(),
            ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode(),
            jobExecutionId);
        log.error(
            "Error converting record to marc "
                + marcRecordEntity.getExternalId()
                + " : "
                + e.getMessage());
        return;
      }
    }
    super.saveConvertJsonRecordToMarcRecordError(marcRecordEntity, jobExecutionId, e);
  }

  /**
   * Retrieves additional MARC fields by external ID for the given records and mapping profile.
   *
   * @param marcRecords List of MARC record entities.
   * @param mappingProfile The mapping profile.
   * @param jobExecutionId The job execution ID.
   * @return Map of UUID to MarcFields.
   * @throws TransformationRuleException if transformation fails.
   */
  @Override
  public Map<UUID, MarcFields> getAdditionalMarcFieldsByExternalId(
      List<MarcRecordEntity> marcRecords, MappingProfile mappingProfile, UUID jobExecutionId)
      throws TransformationRuleException {
    var marcFieldsByExternalId = new HashMap<UUID, MarcFields>();
    if (!holdingsItemsResolver.isNeedUpdateWithHoldingsOrItems(mappingProfile)) {
      return marcFieldsByExternalId;
    }
    var externalIds =
        marcRecords.stream().map(MarcRecordEntity::getExternalId).collect(Collectors.toSet());
    var instanceHridEntities = instanceWithHridEntityRepository.findByIdIn(externalIds);
    entityManager.clear();
    ReferenceDataWrapper referenceData = getReferenceData();
    for (var instanceHridEntity : instanceHridEntities) {
      var holdingsAndItems = new JSONObject();
      holdingsItemsResolver.retrieveHoldingsAndItemsByInstanceId(
          holdingsAndItems,
          instanceHridEntity.getId(),
          instanceHridEntity.getHrid(),
          mappingProfile,
          jobExecutionId);
      var marcFields = mapFields(holdingsAndItems, mappingProfile, referenceData);
      marcFieldsByExternalId.put(instanceHridEntity.getId(), marcFields);
    }
    return marcFieldsByExternalId;
  }

  /**
   * Retrieves a MARC record by its record ID.
   *
   * @param recordId The UUID of the record.
   * @return The MarcRecordEntity.
   */
  @Override
  public MarcRecordEntity getMarcRecord(final UUID recordId) {
    var instances =
        marcRecordEntityRepository.findByExternalIdInAndRecordTypeIsAndStateIn(
            Set.of(recordId), INSTANCE_MARC_TYPE, Set.of("ACTUAL"));
    if (instances.isEmpty()) {
      log.error("getMarcRecord:: Couldn't find instance in db for ID: {}", recordId);
      throw new DownloadRecordException(
          "Couldn't find instance in db for ID: %s".formatted(recordId));
    }
    return instances.get(0);
  }

  /**
   * Retrieves the default mapping profile for instances.
   *
   * @return The default MappingProfile.
   */
  @Override
  public MappingProfile getDefaultMappingProfile() {
    return mappingProfileEntityRepository
        .getReferenceById(UUID.fromString(DEFAULT_INSTANCE_PROFILE_ID))
        .getMappingProfile();
  }

  /**
   * Retrieves rules for the given mapping profile.
   *
   * @param mappingProfile The mapping profile.
   * @return List of Rule.
   * @throws TransformationRuleException if transformation fails.
   */
  private List<Rule> getRules(MappingProfile mappingProfile) throws TransformationRuleException {
    List<Rule> rules;
    if (mappingProfile.getRecordTypes().contains(RecordTypes.SRS)) {
      var defaultMappingProfile =
          mappingProfileEntityRepository
              .getReferenceById(UUID.fromString(DEFAULT_INSTANCE_MAPPING_PROFILE_ID))
              .getMappingProfile();
      var copyDefaultMappingProfile = new MappingProfile();
      copyDefaultMappingProfile.setId(defaultMappingProfile.getId());
      copyDefaultMappingProfile.setDefault(defaultMappingProfile.getDefault());
      copyDefaultMappingProfile.setName(defaultMappingProfile.getName());
      copyDefaultMappingProfile.setRecordTypes(
          new ArrayList<>(defaultMappingProfile.getRecordTypes()));
      if (defaultMappingProfile.getTransformations() != null) {
        copyDefaultMappingProfile.setTransformations(
            new ArrayList<>(defaultMappingProfile.getTransformations()));
      }
      copyDefaultMappingProfile.setDescription(defaultMappingProfile.getDescription());
      copyDefaultMappingProfile.setFieldsSuppression(mappingProfile.getFieldsSuppression());
      copyDefaultMappingProfile.setSuppress999ff(mappingProfile.getSuppress999ff());
      var mappingProfileWithHoldingsAndItems =
          appendHoldingsAndItemTransformations(mappingProfile, copyDefaultMappingProfile);
      rules = ruleFactory.getRules(mappingProfileWithHoldingsAndItems);
    } else {
      rules = ruleFactory.getRules(mappingProfile);
    }
    return rules;
  }

  /**
   * Returns default export identifiers for duplicate error reporting for the given instance ID.
   *
   * @param id The UUID of the instance.
   * @return Optional containing ExportIdentifiersForDuplicateError with default identifier message.
   */
  protected Optional<ExportIdentifiersForDuplicateError> getDefaultIdentifiers(UUID id) {
    var exportIdentifiers = new ExportIdentifiersForDuplicateError();
    exportIdentifiers.setIdentifierHridMessage("Instance with ID : " + id);
    return Optional.of(exportIdentifiers);
  }

  /**
   * Maps fields from a JSON object to MARC fields using the mapping profile and reference data.
   *
   * @param marcRecord The JSON object representing the record.
   * @param mappingProfile The mapping profile.
   * @param referenceData The reference data wrapper.
   * @return MarcFields containing mapped fields.
   * @throws TransformationRuleException if transformation fails.
   */
  private MarcFields mapFields(
      JSONObject marcRecord, MappingProfile mappingProfile, ReferenceDataWrapper referenceData)
      throws TransformationRuleException {
    var rules = ruleFactory.getRules(mappingProfile);
    var finalRules = ruleHandler.preHandle(marcRecord, rules);
    EntityReader entityReader = new JPathSyntaxEntityReader(marcRecord.toJSONString());
    RecordWriter recordWriter = new MarcRecordWriter();
    var marcHoldingsItemsFieldsResult = new MarcFields();
    List<VariableField> mappedRecord =
        ruleProcessor.processFields(
            entityReader,
            recordWriter,
            referenceData,
            finalRules,
            (translationException -> {
              List<String> errorMessageValues =
                  Arrays.asList(
                      translationException.getRecordInfo().getId(),
                      translationException.getErrorCode().getDescription(),
                      translationException.getMessage());
              marcHoldingsItemsFieldsResult.setErrorMessages(errorMessageValues);
            }));
    marcHoldingsItemsFieldsResult.setHoldingItemsFields(mappedRecord);
    return marcHoldingsItemsFieldsResult;
  }

  /**
   * Retrieves instances with holdings and items for the given instance IDs.
   *
   * @param instancesIds Set of instance UUIDs.
   * @param generatedMarcResult The result object to populate.
   * @param mappingProfile The mapping profile.
   * @return List of JSONObjects representing instances with holdings and items.
   */
  protected List<JSONObject> getInstancesWithHoldingsAndItems(
      Set<UUID> instancesIds,
      GeneratedMarcResult generatedMarcResult,
      MappingProfile mappingProfile) {
    var instances = instanceEntityRepository.findByIdIn(instancesIds);
    entityManager.clear();
    return getInstancesWithHoldingsAndItems(
        instancesIds, generatedMarcResult, mappingProfile, instances);
  }

  /**
   * Retrieves instances with holdings and items for the given instance IDs and instance entities.
   *
   * @param instancesIds Set of instance UUIDs.
   * @param generatedMarcResult The result object to populate.
   * @param mappingProfile The mapping profile.
   * @param instances List of instance entities.
   * @return List of JSONObjects representing instances with holdings and items.
   */
  protected List<JSONObject> getInstancesWithHoldingsAndItems(
      Set<UUID> instancesIds,
      GeneratedMarcResult generatedMarcResult,
      MappingProfile mappingProfile,
      List<InstanceEntity> instances) {
    List<JSONObject> instancesWithHoldingsAndItems = new ArrayList<>();
    var copyInstances = new ArrayList<>(instances);
    var foundIds = copyInstances.stream().map(InstanceEntity::getId).collect(Collectors.toSet());
    var notFoundInLocalTenant = new HashSet<>(instancesIds);
    notFoundInLocalTenant.removeIf(foundIds::contains);
    var instancesIdsFromCentral = new HashSet<UUID>();
    if (!notFoundInLocalTenant.isEmpty()
        && !consortiaService.isCurrentTenantCentralTenant(folioExecutionContext.getTenantId())) {
      var centralTenantId =
          consortiaService.getCentralTenantId(folioExecutionContext.getTenantId());
      if (StringUtils.isNotEmpty(centralTenantId)) {
        var instancesFromCentralTenant =
            instanceCentralTenantRepository.findInstancesByIdIn(
                centralTenantId, notFoundInLocalTenant);
        instancesFromCentralTenant.forEach(
            instanceEntity -> {
              copyInstances.add(instanceEntity);
              instancesIdsFromCentral.add(instanceEntity.getId());
            });
      }
    }
    var existInstanceIds = new HashSet<UUID>();
    for (var instance : copyInstances) {
      existInstanceIds.add(instance.getId());
      var instanceJsonOpt = getAsJsonObject(instance.getJsonb());
      if (instanceJsonOpt.isEmpty()) {
        var errorMessage =
            String.format(ERROR_CONVERTING_TO_JSON_INSTANCE.getDescription(), instance.getId());
        log.error("getInstancesWithHoldingsAndItems:: {}", errorMessage);
        generatedMarcResult.addIdToFailed(instance.getId());
        errorLogService.saveGeneralError(errorMessage, generatedMarcResult.getJobExecutionId());
        continue;
      }
      var instanceWithHoldingsAndItems = new JSONObject();
      var instanceJson = instanceJsonOpt.get();
      instanceWithHoldingsAndItems.put(INSTANCE_KEY, instanceJson);
      log.debug("getInstancesWithHoldingsAndItems instanceJson: {}", instanceJson);

      if (!instancesIdsFromCentral.contains(instance.getId())) {
        holdingsItemsResolver.retrieveHoldingsAndItemsByInstanceId(
            instanceWithHoldingsAndItems,
            instance.getId(),
            instanceJson.getAsString(HRID_KEY),
            mappingProfile,
            generatedMarcResult.getJobExecutionId());
      }

      instancesWithHoldingsAndItems.add(instanceWithHoldingsAndItems);
    }
    instancesIds.removeAll(existInstanceIds);
    instancesIds.forEach(
        instanceId -> {
          log.error(
              "getInstancesWithHoldingsAndItems:: instance by id {} does not exist", instanceId);
          generatedMarcResult.addIdToNotExist(instanceId);
          generatedMarcResult.addIdToFailed(instanceId);
        });
    return instancesWithHoldingsAndItems;
  }

  /**
   * Maps a JSON object to a MARC record string using the provided rules and reference data.
   *
   * @param jsonObject The JSON object to map.
   * @param rules List of transformation rules.
   * @param referenceDataWrapper The reference data wrapper.
   * @return The MARC record as a string.
   */
  protected String mapToMarc(
      JSONObject jsonObject, List<Rule> rules, ReferenceDataWrapper referenceDataWrapper) {
    rules = ruleHandler.preHandle(jsonObject, rules);
    EntityReader entityReader = new JPathSyntaxEntityReader(jsonObject.toJSONString());
    RecordWriter recordWriter = new MarcRecordWriter();
    return ruleProcessor.process(
        entityReader,
        recordWriter,
        referenceDataWrapper,
        rules,
        (translationException -> {
          var instanceJson = (JSONObject) jsonObject.get(INSTANCE_KEY);
          log.warn(
              "mapToSrs:: exception: {} for instance {}",
              translationException.getCause().getMessage(),
              instanceJson.getAsString(ID_KEY));
        }));
  }

  /**
   * Appends holdings and item transformations from the mapping profile to the default profile.
   *
   * @param mappingProfile The mapping profile.
   * @param defaultMappingProfile The default mapping profile.
   * @return The updated MappingProfile.
   */
  private MappingProfile appendHoldingsAndItemTransformations(
      MappingProfile mappingProfile, MappingProfile defaultMappingProfile) {
    if (isNotEmpty(mappingProfile.getTransformations())) {
      var updatedRecordTypes = new HashSet<>(defaultMappingProfile.getRecordTypes());
      if (mappingProfile.getRecordTypes().contains(RecordTypes.HOLDINGS)) {
        updatedRecordTypes.add(RecordTypes.HOLDINGS);
      }
      if (mappingProfile.getRecordTypes().contains(RecordTypes.ITEM)) {
        updatedRecordTypes.add(RecordTypes.ITEM);
      }
      defaultMappingProfile.setRecordTypes(new ArrayList<>(updatedRecordTypes));
      defaultMappingProfile.setTransformations(mappingProfile.getTransformations());
    }
    return defaultMappingProfile;
  }

  /**
   * Retrieves reference data for the current tenant and user.
   *
   * @return ReferenceDataWrapper for the current context.
   */
  private ReferenceDataWrapper getReferenceData() {
    ReferenceDataWrapper referenceData;
    if (consortiaService.isCurrentTenantCentralTenant(folioExecutionContext.getTenantId())) {
      referenceData =
          referenceDataProvider.getReference(
              folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString());
    } else {
      referenceData = referenceDataProvider.getReference(folioExecutionContext.getTenantId());
    }
    return referenceData;
  }
}
