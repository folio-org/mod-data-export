package org.folio.dataexp.service.export.strategies;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.ItemEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.InstanceWithHridEntityRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.repository.MarcInstanceRecordRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.processor.RuleProcessor;
import org.folio.processor.referencedata.ReferenceDataWrapper;
import org.folio.processor.rule.Rule;
import org.folio.reader.EntityReader;
import org.folio.reader.JPathSyntaxEntityReader;
import org.folio.writer.RecordWriter;
import org.folio.writer.impl.MarcRecordWriter;
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

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.folio.dataexp.service.export.Constants.DEFAULT_INSTANCE_MAPPING_PROFILE_ID;
import static org.folio.dataexp.service.export.Constants.HOLDINGS_KEY;
import static org.folio.dataexp.service.export.Constants.HRID_KEY;
import static org.folio.dataexp.service.export.Constants.ID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_HRID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_KEY;
import static org.folio.dataexp.service.export.Constants.ITEMS_KEY;

@Log4j2
@Component
@AllArgsConstructor
public class InstancesExportStrategy extends AbstractExportStrategy {

  private static final String INSTANCE_MARC_TYPE = "MARC_BIB";

  private final ConsortiaService consortiaService;
  private final MarcInstanceRecordRepository marcInstanceRecordRepository;
  private final MarcRecordEntityRepository marcRecordEntityRepository;
  private final InstanceEntityRepository instanceEntityRepository;
  private final HoldingsRecordEntityRepository holdingsRecordEntityRepository;
  private final ItemEntityRepository itemEntityRepository;
  private final RuleFactory ruleFactory;
  private final RuleHandler ruleHandler;
  private final RuleProcessor ruleProcessor;
  private final ReferenceDataProvider referenceDataProvider;
  private final MappingProfileEntityRepository mappingProfileEntityRepository;
  private final InstanceWithHridEntityRepository instanceWithHridEntityRepository;

  @Override
  public List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds, MappingProfile mappingProfile) {
    if (Boolean.TRUE.equals(mappingProfile.getDefault()) || mappingProfile.getRecordTypes().contains(RecordTypes.SRS)) {
      var marcInstances =  marcRecordEntityRepository.findByExternalIdInAndRecordTypeIs(externalIds, INSTANCE_MARC_TYPE);
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
  public GeneratedMarcResult getGeneratedMarc(Set<UUID> instanceIds, MappingProfile mappingProfile) {
    var generatedMarcResult = new GeneratedMarcResult();

    var defaultMappingProfile = mappingProfileEntityRepository.getReferenceById(UUID.fromString(DEFAULT_INSTANCE_MAPPING_PROFILE_ID)).getMappingProfile();
    var copyDefaultMappingProfile = new MappingProfile();
    copyDefaultMappingProfile.setId(defaultMappingProfile.getId());
    copyDefaultMappingProfile.setDefault(defaultMappingProfile.getDefault());
    copyDefaultMappingProfile.setName(defaultMappingProfile.getName());
    copyDefaultMappingProfile.setRecordTypes(new ArrayList<>(defaultMappingProfile.getRecordTypes()));
    copyDefaultMappingProfile.setTransformations(new ArrayList<>(defaultMappingProfile.getTransformations()));
    copyDefaultMappingProfile.setDescription(defaultMappingProfile.getDescription());

    var updatedMappingProfile = appendHoldingsAndItemTransformations(mappingProfile, copyDefaultMappingProfile);

    var rules = ruleFactory.getRules(updatedMappingProfile);
    var instancesWithHoldingsAndItems = getInstancesWithHoldingsAndItems(instanceIds, generatedMarcResult, mappingProfile);
    var marcRecords = instancesWithHoldingsAndItems.stream().map(h -> mapToMarc(h, new ArrayList<>(rules))).toList();

    generatedMarcResult.setMarcRecords(marcRecords);
    return generatedMarcResult;
  }

  @Override
  public Optional<String> getIdentifierMessage(UUID id) {
    var instances = instanceEntityRepository.findByIdIn(Set.of(id));
    if (instances.isEmpty()) return Optional.empty();
    var jsonObject =  getAsJsonObject(instances.get(0).getJsonb());
    if (jsonObject.isPresent()) {
      var hrid = jsonObject.get().getAsString(HRID_KEY);
      return Optional.of("Instance with hrid : " + hrid);
    }
    return Optional.empty();
  }

  @Override
  public Map<UUID, MarcFields> getAdditionalMarcFieldsByExternalId(List<MarcRecordEntity> marcRecords, MappingProfile mappingProfile) {
    var marcFieldsByExternalId = new HashMap<UUID, MarcFields>();
    if (!isNeedUpdateWithHoldingsOrItems(mappingProfile)) {
      return marcFieldsByExternalId;
    }
    var externalIds = marcRecords.stream()
      .map(MarcRecordEntity::getExternalId).collect(Collectors.toSet());
    var instanceHridEntities = instanceWithHridEntityRepository.findByIdIn(externalIds);
    for (var instanceHridEntity : instanceHridEntities) {
      var holdingsAndItems = new JSONObject();
      addHoldingsAndItems(holdingsAndItems, instanceHridEntity.getId(), instanceHridEntity.getHrid(), mappingProfile);
      var marcFields = mapFields(holdingsAndItems, mappingProfile);
      marcFieldsByExternalId.put(instanceHridEntity.getId(), marcFields);
    }
    return marcFieldsByExternalId;
  }

  private MarcFields mapFields(JSONObject marcRecord, MappingProfile mappingProfile) {
    ReferenceDataWrapper referenceData = referenceDataProvider.getReference();
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


  protected List<JSONObject> getInstancesWithHoldingsAndItems(Set<UUID> instancesIds, GeneratedMarcResult generatedMarcResult, MappingProfile mappingProfile) {
    List<JSONObject> instancesWithHoldingsAndItems = new ArrayList<>();
    var instances = instanceEntityRepository.findByIdIn(instancesIds);
    var existInstanceIds = new HashSet<UUID>();
    for (var instance : instances) {
      existInstanceIds.add(instance.getId());
      var instanceJsonOpt = getAsJsonObject(instance.getJsonb());
      if (instanceJsonOpt.isEmpty()) {
        log.error("getInstancesWithHoldingsAndItems:: Error converting to json instance by id {}", instance.getId());
        generatedMarcResult.addIdToFailed(instance.getId());
        continue;
      }
      var instanceWithHoldingsAndItems = new JSONObject();
      var instanceJson = instanceJsonOpt.get();
      instanceWithHoldingsAndItems.put(INSTANCE_KEY, instanceJson);
      addHoldingsAndItems(instanceWithHoldingsAndItems, instance.getId(), instanceJson.getAsString(HRID_KEY), mappingProfile);
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
    if (holdingsEntities.isEmpty()) {
      return;
    }
    HashMap<UUID, List<ItemEntity>> itemsByHoldingId = new HashMap<>();
    if (mappingProfile.getRecordTypes().contains(RecordTypes.ITEM)) {
      var ids = holdingsEntities.stream().map(HoldingsRecordEntity::getId).collect(Collectors.toSet());
      itemsByHoldingId  = itemEntityRepository.findByHoldingsRecordIdIn(ids)
        .stream().collect(Collectors.groupingBy(ItemEntity::getHoldingsRecordId,
          HashMap::new, Collectors.mapping(itemEntity -> itemEntity, Collectors.toList())));
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

  private String mapToMarc(JSONObject jsonObject, List<Rule> rules) {
    rules = ruleHandler.preHandle(jsonObject, rules);
    EntityReader entityReader = new JPathSyntaxEntityReader(jsonObject.toJSONString());
    RecordWriter recordWriter = new MarcRecordWriter();
    ReferenceDataWrapper referenceDataWrapper = referenceDataProvider.getReference();
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
