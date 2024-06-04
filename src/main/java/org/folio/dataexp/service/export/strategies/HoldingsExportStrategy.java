package org.folio.dataexp.service.export.strategies;

import static org.folio.dataexp.service.export.Constants.HOLDINGS_KEY;
import static org.folio.dataexp.service.export.Constants.HRID_KEY;
import static org.folio.dataexp.service.export.Constants.ID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_HRID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_KEY;
import static org.folio.dataexp.service.export.Constants.ITEMS_KEY;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.folio.dataexp.client.ConsortiumSearchClient;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.exception.TransformationRuleException;
import org.folio.dataexp.repository.*;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.processor.RuleProcessor;
import org.folio.processor.referencedata.ReferenceDataWrapper;
import org.folio.processor.rule.Rule;
import org.folio.reader.EntityReader;
import org.folio.reader.JPathSyntaxEntityReader;
import org.folio.spring.FolioExecutionContext;
import org.folio.writer.RecordWriter;
import org.folio.writer.impl.MarcRecordWriter;
import org.marc4j.MarcException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
public class HoldingsExportStrategy extends AbstractExportStrategy {
  protected static final String HOLDING_MARC_TYPE = "MARC_HOLDING";

  private final InstanceEntityRepository instanceEntityRepository;
  private final ItemEntityRepository itemEntityRepository;
  private final RuleFactory ruleFactory;
  private final RuleProcessor ruleProcessor;
  private final RuleHandler ruleHandler;
  private final ReferenceDataProvider referenceDataProvider;
  private final ConsortiaService consortiaService;
  private final FolioExecutionContext context;
  private final ConsortiumSearchClient consortiumSearchClient;
  private final HoldingsCentralTenantRepository holdingsCentralTenantRepository;
  private final MarcRecordCentralTenantRepository marcRecordCentralTenantRepository;

  protected final HoldingsRecordEntityRepository holdingsRecordEntityRepository;
  protected final MarcRecordEntityRepository marcRecordEntityRepository;

  @Override
  public List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds, MappingProfile mappingProfile, ExportRequest exportRequest,
                                               UUID jobExecutionId) {
    if (Boolean.TRUE.equals(mappingProfile.getDefault())) {
      var centralTenantId = consortiaService.getCentralTenantId();
      if (centralTenantId.equals(context.getTenantId())) {
        var availableTenants = consortiaService.getAffiliatedTenants();
        Map<String, Set<UUID>> tenantIdsMap = new HashMap<>();
        externalIds.forEach(id -> {
          var curTenant = consortiumSearchClient.getHoldingsById(id.toString()).getTenantId();
          if (availableTenants.contains(curTenant) || curTenant.equals(centralTenantId)) {
            tenantIdsMap.computeIfAbsent(curTenant, k -> new HashSet<>()).add(id);
          }
        });
        List<MarcRecordEntity> entities = new ArrayList<>();
        tenantIdsMap.forEach((k, v) -> entities.addAll(marcRecordCentralTenantRepository.findMarcRecordsByIdIn(k, v)));
        return entities;
      } else {
        return marcRecordEntityRepository.findByExternalIdInAndRecordTypeIsAndStateIs(externalIds,
          HOLDING_MARC_TYPE, "ACTUAL");
      }
    }
    return new ArrayList<>();
  }

  @Override
  public GeneratedMarcResult getGeneratedMarc(Set<UUID> holdingsIds, MappingProfile mappingProfile, ExportRequest exportRequest,
      UUID jobExecutionId, ExportStrategyStatistic exportStatistic) {
    var result = new GeneratedMarcResult(jobExecutionId);
    var holdingsWithInstanceAndItems = getHoldingsWithInstanceAndItems(holdingsIds, result, mappingProfile);
    return getGeneratedMarc(mappingProfile, holdingsWithInstanceAndItems, jobExecutionId, result);
  }

  @Override
  Optional<ExportIdentifiersForDuplicateErrors> getIdentifiers(UUID id) {
    var holdings = holdingsRecordEntityRepository.findByIdIn(Set.of(id));
    if (holdings.isEmpty()) return Optional.empty();
    var jsonObject =  getAsJsonObject(holdings.get(0).getJsonb());
    if (jsonObject.isPresent()) {
      var hrid = jsonObject.get().getAsString(HRID_KEY);
      var exportIdentifiers = new ExportIdentifiersForDuplicateErrors();
      exportIdentifiers.setIdentifierHridMessage("Holding with hrid : " + hrid);
      return Optional.of(exportIdentifiers);
    }
    return Optional.empty();
  }

  protected GeneratedMarcResult getGeneratedMarc(MappingProfile mappingProfile, List<JSONObject> holdingsWithInstanceAndItems,
      UUID jobExecutionId, GeneratedMarcResult result) {
    List<Rule> rules;
    try {
      rules = ruleFactory.getRules(mappingProfile);
    } catch (TransformationRuleException e) {
      log.error(e);
      errorLogService.saveGeneralError(e.getMessage(), jobExecutionId);
      return result;
    }
    var marcRecords = new ArrayList<String>();
    ReferenceDataWrapper referenceDataWrapper = referenceDataProvider.getReference();
    for (var jsonObject : holdingsWithInstanceAndItems) {
      try {
        var marc = mapToMarc(jsonObject, rules, referenceDataWrapper);
        marcRecords.add(marc);
      } catch (MarcException e) {
        var holdingsArray = (JSONArray) jsonObject.get(HOLDINGS_KEY);
        var holdingsJsonObject = (JSONObject) holdingsArray.get(0);
        var uuid = holdingsJsonObject.getAsString(ID_KEY);
        result.addIdToFailed(UUID.fromString(uuid));
        var errorMessage = String.format("%s for holding %s", e.getMessage(), uuid);
        errorLogService.saveGeneralErrorWithMessageValues(ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode(), List.of(errorMessage), jobExecutionId);
        log.error(" getGeneratedMarc::  exception to convert in marc: {}", errorMessage);
      }
    }
    result.setMarcRecords(marcRecords);
    return result;
  }

  @Override
  public Map<UUID,MarcFields> getAdditionalMarcFieldsByExternalId(List<MarcRecordEntity> marcRecords, MappingProfile mappingProfile) {
    return new HashMap<>();
  }

  protected List<JSONObject> getHoldingsWithInstanceAndItems(Set<UUID> holdingsIds, GeneratedMarcResult result, MappingProfile mappingProfile) {
    var holdings = holdingsRecordEntityRepository.findByIdIn(holdingsIds);
    var instancesIds = holdings.stream().map(HoldingsRecordEntity::getInstanceId).collect(Collectors.toSet());
    return getHoldingsWithInstanceAndItems(holdingsIds, result, mappingProfile, holdings, instancesIds);
  }

  protected List<JSONObject> getHoldingsWithInstanceAndItems(Set<UUID> holdingsIds, GeneratedMarcResult generatedMarcResult, MappingProfile mappingProfile,
                                                             List<HoldingsRecordEntity> holdings, Set<UUID> instancesIds) {
    var instances = instanceEntityRepository.findByIdIn(instancesIds);
    entityManager.clear();
    List<JSONObject> holdingsWithInstanceAndItems = new ArrayList<>();
    var existHoldingsIds = new HashSet<UUID>();
    for (var holding : holdings) {
      existHoldingsIds.add(holding.getId());
      var holdingJsonOpt = getAsJsonObject(holding.getJsonb());
      if (holdingJsonOpt.isEmpty()) {
        var errorMessage = "Error converting to json holding by id " + holding.getId();
        log.error("getHoldingsWithInstanceAndItems:: {}", errorMessage);
        generatedMarcResult.addIdToFailed(holding.getId());
        errorLogService.saveGeneralError(errorMessage, generatedMarcResult.getJobExecutionId());
        continue;
      }
      var holdingJson = holdingJsonOpt.get();
      var holdingWithInstanceAndItems = new JSONObject();
      for (var instance : instances) {
        if (instance.getId().equals(holding.getInstanceId())) {
          var instanceJsonOpt = getAsJsonObject(instance.getJsonb());
          if (instanceJsonOpt.isEmpty()) {
            log.error("getHoldingsWithInstanceAndItems:: Error converting to json instance by id {}", instance.getId());
          } else {
            var instanceJson = instanceJsonOpt.get();
            holdingWithInstanceAndItems.appendField(INSTANCE_KEY, instanceJson);
            holdingJson.put(INSTANCE_HRID_KEY, instanceJson.getAsString(HRID_KEY));
            break;
          }
        }
      }
      if (mappingProfile.getRecordTypes().contains(RecordTypes.ITEM)) {
        addItemsToHolding(holdingJson, holding.getId());
      }
      var holdingJsonArray = new JSONArray();
      holdingJsonArray.add(holdingJson);
      holdingWithInstanceAndItems.put(HOLDINGS_KEY, holdingJsonArray);
      holdingsWithInstanceAndItems.add(holdingWithInstanceAndItems);
    }
    holdingsIds.removeAll(existHoldingsIds);
    holdingsIds.forEach(
      holdingsId -> {
        log.error("getHoldingsWithInstanceAndItems:: holding by id {} does not exist", holdingsId);
        generatedMarcResult.addIdToNotExist(holdingsId);
        generatedMarcResult.addIdToFailed(holdingsId);
      });
    return holdingsWithInstanceAndItems;
  }

  private String mapToMarc(JSONObject jsonObject, List<Rule> rules, ReferenceDataWrapper referenceDataWrapper) {
    rules = ruleHandler.preHandle(jsonObject, rules);
    EntityReader entityReader = new JPathSyntaxEntityReader(jsonObject.toJSONString());
    RecordWriter recordWriter = new MarcRecordWriter();
    return ruleProcessor.process(entityReader, recordWriter, referenceDataWrapper, rules, (translationException -> {
      var holdingsArray = (JSONArray) jsonObject.get(HOLDINGS_KEY);
      var holdingsJsonObject = (JSONObject) holdingsArray.get(0);
      log.warn("mapToSrs:: exception: {} for holding {}", translationException.getCause().getMessage(), holdingsJsonObject.getAsString(ID_KEY));
    }));
  }

  private void addItemsToHolding(JSONObject holdingJson, UUID holdingId) {
    var items = itemEntityRepository.findByHoldingsRecordIdIs(holdingId);
    var itemJsonArray = new JSONArray();
    items.forEach(itemEntity -> {
      var itemJsonOpt = getAsJsonObject(itemEntity.getJsonb());
      if (itemJsonOpt.isPresent()) {
        itemJsonArray.add(itemJsonOpt.get());
      } else {
        log.error("addItemsToHolding:: error converting to json item by id {}", itemEntity.getId());
      }
    });
    holdingJson.put(ITEMS_KEY, itemJsonArray);
  }
}
