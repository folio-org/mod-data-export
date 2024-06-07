package org.folio.dataexp.service.export.strategies;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.folio.dataexp.service.export.Constants.DEFAULT_INSTANCE_MAPPING_PROFILE_ID;
import static org.folio.dataexp.service.export.Constants.HOLDINGS_KEY;
import static org.folio.dataexp.service.export.Constants.HRID_KEY;
import static org.folio.dataexp.service.export.Constants.ID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_HRID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_KEY;
import static org.folio.dataexp.service.export.Constants.ITEMS_KEY;
import static org.folio.dataexp.service.export.Constants.TITLE_KEY;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.folio.dataexp.domain.entity.ItemEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.exception.TransformationRuleException;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.InstanceCentralTenantRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.InstanceWithHridEntityRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
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
import org.folio.spring.FolioExecutionContext;
import org.folio.writer.RecordWriter;
import org.folio.writer.impl.MarcRecordWriter;
import org.marc4j.MarcException;
import org.marc4j.marc.VariableField;
import org.springframework.stereotype.Component;

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

@Log4j2
@Component
@AllArgsConstructor
public class InstancesExportStrategy extends AbstractExportStrategy {

  protected static final String INSTANCE_MARC_TYPE = "MARC_BIB";
  protected static final String LONG_MARC_RECORD_MESSAGE = "Record is too long to be a valid MARC binary record";
  private final ConsortiaService consortiaService;
  private final InstanceCentralTenantRepository instanceCentralTenantRepository;
  private final MarcInstanceRecordRepository marcInstanceRecordRepository;
  private final HoldingsRecordEntityRepository holdingsRecordEntityRepository;
  private final ItemEntityRepository itemEntityRepository;
  private final RuleFactory ruleFactory;
  private final RuleHandler ruleHandler;
  private final RuleProcessor ruleProcessor;
  private final ReferenceDataProvider referenceDataProvider;
  private final MappingProfileEntityRepository mappingProfileEntityRepository;
  private final InstanceWithHridEntityRepository instanceWithHridEntityRepository;
  private final FolioExecutionContext folioExecutionContext;

  protected final MarcRecordEntityRepository marcRecordEntityRepository;
  protected final InstanceEntityRepository instanceEntityRepository;

  @Override
  public List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds, MappingProfile mappingProfile, ExportRequest exportRequest,
                                               UUID jobExecutionId) {
    if (Boolean.TRUE.equals(mappingProfile.getDefault()) || mappingProfile.getRecordTypes().contains(RecordTypes.SRS)) {
      var marcInstances =  marcRecordEntityRepository.findByExternalIdInAndRecordTypeIsAndStateIs(externalIds, INSTANCE_MARC_TYPE, "ACTUAL");
      var foundIds = marcInstances.stream().map(MarcRecordEntity::getExternalId).collect(Collectors.toSet());
      externalIds.removeAll(foundIds);
      if (!externalIds.isEmpty()) {
        var centralTenantId = consortiaService.getCentralTenantId();
        if (StringUtils.isNotEmpty(centralTenantId)) {
          var marcInstancesFromCentralTenant = marcInstanceRecordRepository.findByExternalIdIn(centralTenantId, externalIds);
          marcInstances.addAll(marcInstancesFromCentralTenant);
        } else {
          log.info("Central tenant id does not exist");
        }
      }
      return marcInstances;
    }
    return new ArrayList<>();
  }

  @Override
  public GeneratedMarcResult getGeneratedMarc(Set<UUID> instanceIds, MappingProfile mappingProfile, ExportRequest exportRequest,
      UUID jobExecutionId, ExportStrategyStatistic exportStatistic) {
    var generatedMarcResult = new GeneratedMarcResult(jobExecutionId);
    var instancesWithHoldingsAndItems = getInstancesWithHoldingsAndItems(instanceIds, generatedMarcResult, mappingProfile);
    return getGeneratedMarc(generatedMarcResult, instancesWithHoldingsAndItems, mappingProfile, jobExecutionId);
  }

  protected GeneratedMarcResult getGeneratedMarc(GeneratedMarcResult generatedMarcResult, List<JSONObject> instancesWithHoldingsAndItems,
      MappingProfile mappingProfile, UUID jobExecutionId) {
    var marcRecords = new ArrayList<String>();
    ReferenceDataWrapper referenceDataWrapper = referenceDataProvider.getReference(folioExecutionContext.getTenantId());
    List<Rule> rules;
    try {
      rules = getRules(mappingProfile);
    } catch (TransformationRuleException e) {
      log.error(e);
      errorLogService.saveGeneralError(e.getMessage(), jobExecutionId);
      return generatedMarcResult;
    }
    for (var jsonObject :  instancesWithHoldingsAndItems) {
      try {
        var marc = mapToMarc(jsonObject, rules, referenceDataWrapper);
        marcRecords.add(marc);
      } catch (MarcException e) {
        var instanceJson = (JSONObject)jsonObject.get(INSTANCE_KEY);
        var uuid = instanceJson.getAsString(ID_KEY);
        generatedMarcResult.addIdToFailed(UUID.fromString(uuid));
        errorLogService.saveWithAffectedRecord(instanceJson, ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode(), jobExecutionId, e);
        log.error(" getGeneratedMarc:: exception to convert in marc : {} for instance {}", e.getMessage(), uuid);
      }
    }
    generatedMarcResult.setMarcRecords(marcRecords);
    return generatedMarcResult;
  }

  @Override
  public Optional<ExportIdentifiersForDuplicateErrors> getIdentifiers(UUID id) {
    var instances = instanceEntityRepository.findByIdIn(Set.of(id));
    if (instances.isEmpty()) {
      log.info("getIdentifiers:: not found for instance by id {}", id);
      return getDefaultIdentifiers(id);
    }
    var jsonObject =  getAsJsonObject(instances.get(0).getJsonb());
    if (jsonObject.isPresent()) {
      var hrid = jsonObject.get().getAsString(HRID_KEY);
      var title = jsonObject.get().getAsString(TITLE_KEY);
      var uuid = jsonObject.get().getAsString(ID_KEY);
      var exportIdentifiers = new ExportIdentifiersForDuplicateErrors();
      exportIdentifiers.setIdentifierHridMessage("Instance with HRID : " + hrid);
      var instanceAssociatedJsonObject = new JSONObject();
      instanceAssociatedJsonObject.put(ErrorLogService.ID, uuid);
      instanceAssociatedJsonObject.put(ErrorLogService.HRID, hrid);
      instanceAssociatedJsonObject.put(ErrorLogService.TITLE, title);
      exportIdentifiers.setAssociatedJsonObject(instanceAssociatedJsonObject);
      return Optional.of(exportIdentifiers);
    }
    return getDefaultIdentifiers(id);
  }

  @Override
  public void saveConvertJsonRecordToMarcRecordError(MarcRecordEntity marcRecordEntity, UUID jobExecutionId, Exception e) {
    var errorMessage = e.getMessage();
    var instances = instanceEntityRepository.findByIdIn(Set.of(marcRecordEntity.getExternalId()));
    if (errorMessage.contains(LONG_MARC_RECORD_MESSAGE) && !instances.isEmpty()) {
      var jsonObject= getAsJsonObject(instances.get(0).getJsonb());
      if (jsonObject.isPresent()) {
        var instanceJson= jsonObject.get();
        errorLogService.saveWithAffectedRecord(instanceJson, e.getMessage(), ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode(), jobExecutionId);
        log.error("Error converting record to marc " + marcRecordEntity.getExternalId() + " : " + e.getMessage());
        return;
      }
    }
    super.saveConvertJsonRecordToMarcRecordError(marcRecordEntity, jobExecutionId, e);
  }

  protected Optional<ExportIdentifiersForDuplicateErrors> getDefaultIdentifiers(UUID id) {
    var exportIdentifiers = new ExportIdentifiersForDuplicateErrors();
    exportIdentifiers.setIdentifierHridMessage("Instance with ID : " + id);
    return Optional.of(exportIdentifiers);
  }

  private List<Rule> getRules(MappingProfile mappingProfile) throws TransformationRuleException {
    List<Rule> rules;
    if (mappingProfile.getRecordTypes().contains(RecordTypes.SRS)) {
      var defaultMappingProfile = mappingProfileEntityRepository.getReferenceById(UUID.fromString(DEFAULT_INSTANCE_MAPPING_PROFILE_ID)).getMappingProfile();
      var copyDefaultMappingProfile = new MappingProfile();
      copyDefaultMappingProfile.setId(defaultMappingProfile.getId());
      copyDefaultMappingProfile.setDefault(defaultMappingProfile.getDefault());
      copyDefaultMappingProfile.setName(defaultMappingProfile.getName());
      copyDefaultMappingProfile.setRecordTypes(new ArrayList<>(defaultMappingProfile.getRecordTypes()));
      if (defaultMappingProfile.getTransformations() != null) {
        copyDefaultMappingProfile.setTransformations(new ArrayList<>(defaultMappingProfile.getTransformations()));
      }
      copyDefaultMappingProfile.setDescription(defaultMappingProfile.getDescription());
      copyDefaultMappingProfile.setFieldsSuppression(mappingProfile.getFieldsSuppression());
      copyDefaultMappingProfile.setSuppress999ff(mappingProfile.getSuppress999ff());
      var mappingProfileWithHoldingsAndItems = appendHoldingsAndItemTransformations(mappingProfile, copyDefaultMappingProfile);
      rules = ruleFactory.getRules(mappingProfileWithHoldingsAndItems);
    } else {
      rules = ruleFactory.getRules(mappingProfile);
    }
    return rules;
  }

  @Override
  public Map<UUID, MarcFields> getAdditionalMarcFieldsByExternalId(List<MarcRecordEntity> marcRecords, MappingProfile mappingProfile) throws TransformationRuleException {
    var marcFieldsByExternalId = new HashMap<UUID, MarcFields>();
    if (!isNeedUpdateWithHoldingsOrItems(mappingProfile)) {
      return marcFieldsByExternalId;
    }
    var externalIds = marcRecords.stream()
      .map(MarcRecordEntity::getExternalId).collect(Collectors.toSet());
    var instanceHridEntities = instanceWithHridEntityRepository.findByIdIn(externalIds);
    entityManager.clear();
    for (var instanceHridEntity : instanceHridEntities) {
      var holdingsAndItems = new JSONObject();
      addHoldingsAndItems(holdingsAndItems, instanceHridEntity.getId(), instanceHridEntity.getHrid(), mappingProfile);
      var marcFields = mapFields(holdingsAndItems, mappingProfile);
      marcFieldsByExternalId.put(instanceHridEntity.getId(), marcFields);
    }
    return marcFieldsByExternalId;
  }

  private MarcFields mapFields(JSONObject marcRecord, MappingProfile mappingProfile) throws TransformationRuleException {
    ReferenceDataWrapper referenceData = referenceDataProvider.getReference(folioExecutionContext.getTenantId());
    var rules = ruleFactory.getRules(mappingProfile);
    var finalRules = ruleHandler.preHandle(marcRecord, rules);
    EntityReader entityReader = new JPathSyntaxEntityReader(marcRecord.toJSONString());
    RecordWriter recordWriter = new MarcRecordWriter();
    var marcHoldingsItemsFieldsResult  = new MarcFields();
    List<VariableField> mappedRecord = ruleProcessor
      .processFields(entityReader, recordWriter, referenceData, finalRules, (translationException -> {
        List<String> errorMessageValues = Arrays
          .asList(translationException.getRecordInfo().getId(), translationException.getErrorCode().getDescription(),
            translationException.getMessage());
        marcHoldingsItemsFieldsResult.setErrorMessages(errorMessageValues);
      }));
    marcHoldingsItemsFieldsResult.setHoldingItemsFields(mappedRecord);
    return marcHoldingsItemsFieldsResult;
  }

  protected List<JSONObject> getInstancesWithHoldingsAndItems(Set<UUID> instancesIds, GeneratedMarcResult generatedMarcResult,
      MappingProfile mappingProfile) {
    var instances = instanceEntityRepository.findByIdIn(instancesIds);
    entityManager.clear();
    return getInstancesWithHoldingsAndItems(instancesIds, generatedMarcResult, mappingProfile, instances);
  }

  protected List<JSONObject> getInstancesWithHoldingsAndItems(Set<UUID> instancesIds, GeneratedMarcResult generatedMarcResult,
                                                              MappingProfile mappingProfile, List<InstanceEntity> instances) {
    List<JSONObject> instancesWithHoldingsAndItems = new ArrayList<>();
    var copyInstances = new ArrayList<>(instances);
    var foundIds = copyInstances.stream().map(InstanceEntity::getId).collect(Collectors.toSet());
    var notFoundInLocalTenant = new HashSet<>(instancesIds);
    notFoundInLocalTenant.removeIf(foundIds::contains);
    var instancesIdsFromCentral = new HashSet<UUID>();
    if (!notFoundInLocalTenant.isEmpty()) {
      var centralTenantId = consortiaService.getCentralTenantId();
      if (StringUtils.isNotEmpty(centralTenantId)) {
        var instancesFromCentralTenant = instanceCentralTenantRepository.findInstancesByIdIn(centralTenantId, notFoundInLocalTenant);
        instancesFromCentralTenant.forEach(instanceEntity -> {
          copyInstances.add(instanceEntity);
          instancesIdsFromCentral.add(instanceEntity.getId());
        });
      } else {
        log.info("Central tenant id does not exist");
      }
    }
    var existInstanceIds = new HashSet<UUID>();
    for (var instance : copyInstances) {
      existInstanceIds.add(instance.getId());
      var instanceJsonOpt = getAsJsonObject(instance.getJsonb());
      if (instanceJsonOpt.isEmpty()) {
        var errorMessage = "Error converting to json instance by id " + instance.getId();
        log.error("getInstancesWithHoldingsAndItems:: {}", errorMessage);
        generatedMarcResult.addIdToFailed(instance.getId());
        errorLogService.saveGeneralError(errorMessage, generatedMarcResult.getJobExecutionId());
        continue;
      }
      var instanceWithHoldingsAndItems = new JSONObject();
      var instanceJson = instanceJsonOpt.get();
      instanceWithHoldingsAndItems.put(INSTANCE_KEY, instanceJson);
      if (!instancesIdsFromCentral.contains(instance.getId())) {
        addHoldingsAndItems(instanceWithHoldingsAndItems, instance.getId(), instanceJson.getAsString(HRID_KEY), mappingProfile);
      }
      instancesWithHoldingsAndItems.add(instanceWithHoldingsAndItems);
    }
    instancesIds.removeAll(existInstanceIds);
    instancesIds.forEach(
      instanceId -> {
        log.error("getInstancesWithHoldingsAndItems:: instance by id {} does not exist", instanceId);
        generatedMarcResult.addIdToNotExist(instanceId);
        generatedMarcResult.addIdToFailed(instanceId);
      });
    return instancesWithHoldingsAndItems;
  }

  private void addHoldingsAndItems(JSONObject jsonToUpdateWithHoldingsAndItems, UUID instanceId,
                                     String instanceHrid, MappingProfile mappingProfile) {
    if (!isNeedUpdateWithHoldingsOrItems(mappingProfile)) {
      return;
    }
    var holdingsEntities = holdingsRecordEntityRepository.findByInstanceIdIs(instanceId);
    entityManager.clear();
    if (holdingsEntities.isEmpty()) {
      return;
    }
    HashMap<UUID, List<ItemEntity>> itemsByHoldingId = new HashMap<>();
    if (mappingProfile.getRecordTypes().contains(RecordTypes.ITEM)) {
      var ids = holdingsEntities.stream().map(HoldingsRecordEntity::getId).collect(Collectors.toSet());
      itemsByHoldingId  = itemEntityRepository.findByHoldingsRecordIdIn(ids)
        .stream().collect(Collectors.groupingBy(ItemEntity::getHoldingsRecordId,
          HashMap::new, Collectors.mapping(itemEntity -> itemEntity, Collectors.toList())));
      entityManager.clear();
    }
    var holdingsJsonArray = new JSONArray();
    for (var holdingsEntity : holdingsEntities) {
      var itemJsonArray = new JSONArray();
      var itemEntities = itemsByHoldingId.getOrDefault(holdingsEntity.getId(), new ArrayList<>());
      itemEntities.forEach(itemEntity -> {
        var itemJsonOpt = getAsJsonObject(itemEntity.getJsonb());
        if (itemJsonOpt.isPresent()) {
          itemJsonArray.add(itemJsonOpt.get());
        } else {
          log.error("addItemsToHolding:: error converting to json item by id {}", itemEntity.getId());
        }
      });
      var holdingJsonOpt = getAsJsonObject(holdingsEntity.getJsonb());
      if (holdingJsonOpt.isPresent()) {
        var holdingJson = holdingJsonOpt.get();
        holdingJson.put(ITEMS_KEY, itemJsonArray);
        holdingJson.put(INSTANCE_HRID_KEY, instanceHrid);
        holdingsJsonArray.add(holdingJson);
      } else {
        log.error("addItemsToHolding:: error converting to json holding by id {}", holdingsEntity.getId());
      }
    }
    jsonToUpdateWithHoldingsAndItems.put(HOLDINGS_KEY, holdingsJsonArray);
  }

  protected String mapToMarc(JSONObject jsonObject, List<Rule> rules, ReferenceDataWrapper referenceDataWrapper) {
    rules = ruleHandler.preHandle(jsonObject, rules);
    EntityReader entityReader = new JPathSyntaxEntityReader(jsonObject.toJSONString());
    RecordWriter recordWriter = new MarcRecordWriter();
    return ruleProcessor.process(entityReader, recordWriter, referenceDataWrapper, rules, (translationException -> {
      var instanceJson = (JSONObject)jsonObject.get(INSTANCE_KEY);
      log.warn("mapToSrs:: exception: {} for instance {}", translationException.getCause().getMessage(), instanceJson.getAsString(ID_KEY));
    }));
  }

  private MappingProfile appendHoldingsAndItemTransformations(MappingProfile mappingProfile, MappingProfile defaultMappingProfile) {
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

  private boolean isNeedUpdateWithHoldingsOrItems(MappingProfile mappingProfile) {
    var recordTypes = mappingProfile.getRecordTypes();
    return recordTypes.contains(RecordTypes.HOLDINGS) || recordTypes.contains(RecordTypes.ITEM);
  }
}
