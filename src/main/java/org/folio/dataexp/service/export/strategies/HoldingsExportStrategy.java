package org.folio.dataexp.service.export.strategies;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.folio.dataexp.service.export.Constants.HOLDINGS_KEY;
import static org.folio.dataexp.service.export.Constants.HRID_KEY;
import static org.folio.dataexp.service.export.Constants.ID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_HRID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_KEY;
import static org.folio.dataexp.service.export.Constants.ITEMS_KEY;
import static org.folio.dataexp.util.ErrorCode.ERROR_CONVERTING_TO_JSON_HOLDING;
import static org.folio.dataexp.util.ErrorCode.ERROR_HOLDINGS_NO_PERMISSION;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_NO_AFFILIATION;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_TENANT_NOT_FOUND_FOR_HOLDING;
import static org.folio.dataexp.util.FolioExecutionContextUtil.prepareContextForTenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.folio.dataexp.client.ConsortiumSearchClient;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.exception.TransformationRuleException;
import org.folio.dataexp.repository.HoldingsRecordEntityTenantRepository;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.InstanceCentralTenantRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.repository.MarcInstanceRecordRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.ConsortiaService;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.dataexp.service.validators.PermissionsValidator;
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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Component
@RequiredArgsConstructor
public class HoldingsExportStrategy extends AbstractExportStrategy {
  protected static final String HOLDING_MARC_TYPE = "MARC_HOLDING";

  private final InstanceEntityRepository instanceEntityRepository;
  private final ItemEntityRepository itemEntityRepository;
  private final RuleFactory ruleFactory;
  private final RuleProcessor ruleProcessor;
  private final RuleHandler ruleHandler;
  private final ReferenceDataProvider referenceDataProvider;
  private final ConsortiaService consortiaService;
  private final ConsortiumSearchClient consortiumSearchClient;
  private final HoldingsRecordEntityTenantRepository holdingsRecordEntityTenantRepository;
  private final MarcInstanceRecordRepository marcInstanceRecordRepository;
  private final InstanceCentralTenantRepository instanceCentralTenantRepository;
  private final FolioModuleMetadata folioModuleMetadata;
  private final UserService userService;

  protected final HoldingsRecordEntityRepository holdingsRecordEntityRepository;
  protected final MarcRecordEntityRepository marcRecordEntityRepository;
  protected final PermissionsValidator permissionsValidator;

  private Map<String, Set<UUID>> tenantIdsMap;

  @Override
  public List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds, MappingProfile mappingProfile, ExportRequest exportRequest,
                                               UUID jobExecutionId) {
    if (Boolean.TRUE.equals(mappingProfile.getDefault())) {
      var centralTenantId = consortiaService.getCentralTenantId(folioExecutionContext.getTenantId());
      if (centralTenantId.equals(folioExecutionContext.getTenantId())) {
        tenantIdsMap = getTenantIds(externalIds, centralTenantId, jobExecutionId);
        List<MarcRecordEntity> entities = new ArrayList<>();
        tenantIdsMap.forEach((k, v) -> entities.addAll(marcInstanceRecordRepository.findByExternalIdIn(k, v)));
        return entities;
      } else {
        return marcRecordEntityRepository.findByExternalIdInAndRecordTypeIsAndStateIn(externalIds,
          HOLDING_MARC_TYPE, Set.of("ACTUAL", "DELETED"));
      }
    }
    return new ArrayList<>();
  }

  @Override
  public GeneratedMarcResult getGeneratedMarc(Set<UUID> holdingsIds, MappingProfile mappingProfile, ExportRequest exportRequest,
      UUID jobExecutionId, ExportStrategyStatistic exportStatistic) {
    var result = new GeneratedMarcResult(jobExecutionId);
    var holdingsWithInstanceAndItems = getHoldingsWithInstanceAndItems(holdingsIds, result, mappingProfile, jobExecutionId);
    return getGeneratedMarc(mappingProfile, holdingsWithInstanceAndItems, jobExecutionId, result);
  }

  @Override
  Optional<ExportIdentifiersForDuplicateError> getIdentifiers(UUID id) {
    var holdings = holdingsRecordEntityRepository.findByIdIn(Set.of(id));
    if (holdings.isEmpty()) return Optional.empty();
    var jsonObject =  getAsJsonObject(holdings.get(0).getJsonb());
    if (jsonObject.isPresent()) {
      var hrid = jsonObject.get().getAsString(HRID_KEY);
      var exportIdentifiers = new ExportIdentifiersForDuplicateError();
      exportIdentifiers.setIdentifierHridMessage(hrid);
      return Optional.of(exportIdentifiers);
    }
    return Optional.empty();
  }

  protected GeneratedMarcResult getGeneratedMarc(MappingProfile mappingProfile, Map<UUID, JSONObject> holdingsWithInstanceAndItems,
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
    fillOutMarcRecords(holdingsWithInstanceAndItems, jobExecutionId, marcRecords, result, rules);
    result.setMarcRecords(marcRecords);
    return result;
  }

  @Override
  public Map<UUID,MarcFields> getAdditionalMarcFieldsByExternalId(List<MarcRecordEntity> marcRecords, MappingProfile mappingProfile, UUID jobExecutionId) {
    return new HashMap<>();
  }

  protected Map<UUID, JSONObject> getHoldingsWithInstanceAndItems(Set<UUID> holdingsIds, GeneratedMarcResult result, MappingProfile mappingProfile, UUID jobExecutionId) {
    var holdings = getHoldings(holdingsIds, jobExecutionId);
    var instancesIds = holdings.stream().map(HoldingsRecordEntity::getInstanceId).collect(Collectors.toSet());
    return getHoldingsWithInstanceAndItems(holdingsIds, result, mappingProfile, holdings, instancesIds);
  }

  protected Map<UUID, JSONObject> getHoldingsWithInstanceAndItems(Set<UUID> holdingsIds, GeneratedMarcResult generatedMarcResult, MappingProfile mappingProfile,
                                                             List<HoldingsRecordEntity> holdings, Set<UUID> instancesIds) {
    var instances = getInstances(instancesIds, holdings);
    entityManager.clear();
    Map<UUID, JSONObject> holdingsWithInstanceAndItems = new LinkedHashMap<>();
    var existHoldingsIds = new HashSet<UUID>();
    for (var holding : holdings) {
      existHoldingsIds.add(holding.getId());
      var holdingJsonOpt = getAsJsonObject(holding.getJsonb());
      if (holdingJsonOpt.isEmpty()) {
        var errorMessage = String.format(ERROR_CONVERTING_TO_JSON_HOLDING.getDescription(), holding.getId());
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
      holdingsWithInstanceAndItems.put(holding.getId(), holdingWithInstanceAndItems);
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

  private List<HoldingsRecordEntity> getHoldings(Set<UUID> holdingsIds, UUID jobExecutionId) {
    var centralTenantId = consortiaService.getCentralTenantId(folioExecutionContext.getTenantId());
    if (nonNull(centralTenantId) && centralTenantId.equals(folioExecutionContext.getTenantId())) {
      List<HoldingsRecordEntity> entities = new ArrayList<>();
      if (isNull(tenantIdsMap)) {
        tenantIdsMap = getTenantIds(holdingsIds, centralTenantId, jobExecutionId);
      }
      tenantIdsMap.forEach((k, v) -> entities.addAll(holdingsRecordEntityTenantRepository.findByIdIn(k, v)));
      tenantIdsMap = null;
      return entities;
    }
    return holdingsRecordEntityRepository.findByIdIn(holdingsIds);
  }

  private List<InstanceEntity> getInstances(Set<UUID> instanceIds, List<HoldingsRecordEntity> holdings) {
    var centralTenantId = consortiaService.getCentralTenantId(folioExecutionContext.getTenantId());
    if (nonNull(centralTenantId) && centralTenantId.equals(folioExecutionContext.getTenantId())) {
      Map<UUID, String> instIdTenantMap = getInstanceIdsTenant(holdings, centralTenantId);
      log.info("instIdTenantMap: {}", instIdTenantMap);
      List<InstanceEntity> entities = new ArrayList<>();
      instIdTenantMap.forEach((k, v) -> entities.addAll(instanceCentralTenantRepository.findInstancesByIdIn(v, Set.of(k))));
      log.info("entities: {}", entities);
      return entities;
    }
    return instanceEntityRepository.findByIdIn(instanceIds);
  }

  private Map<String, Set<UUID>> getTenantIds(Set<UUID> ids, String centralTenantId, UUID jobExecutionId) {
    log.info("getTenantIds ids: {}", ids);
    Map<String, Set<UUID>> idsMap = new HashMap<>();
    var availableTenants = consortiaService.getAffiliatedTenants(folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString());
    log.info("Affiliated tenants for user {} from {} tenant: {}", folioExecutionContext.getUserId(), folioExecutionContext.getTenantId(), availableTenants);
    ids.forEach(id -> {
      var curTenant = consortiumSearchClient.getHoldingsById(id.toString()).getTenantId();
      log.info("ID: {}, tenant: {}, actualTenant: {}", id, curTenant, folioExecutionContext.getTenantId());
      if (nonNull(curTenant)) {
        if (availableTenants.contains(curTenant) || curTenant.equals(centralTenantId)) {
          if (permissionsValidator.checkInstanceViewPermissions(curTenant)) {
            idsMap.computeIfAbsent(curTenant, k -> new HashSet<>()).add(id);
          } else {
            var msgValues = List.of(id.toString(), userService.getUserName(folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString()),
              curTenant);
            errorLogService.saveGeneralErrorWithMessageValues(ERROR_HOLDINGS_NO_PERMISSION.getCode(), msgValues, jobExecutionId);
            log.error(format(ERROR_HOLDINGS_NO_PERMISSION.getDescription(), msgValues.toArray()));
          }
        } else {
          var msgValues = List.of(id.toString(), userService.getUserName(folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString()), curTenant);
          errorLogService.saveGeneralErrorWithMessageValues(ERROR_MESSAGE_NO_AFFILIATION.getCode(), msgValues, jobExecutionId);
          log.error(format(ERROR_MESSAGE_NO_AFFILIATION.getDescription(), id, folioExecutionContext.getUserId(), curTenant));
        }
      } else {
        errorLogService.saveGeneralErrorWithMessageValues(ERROR_MESSAGE_TENANT_NOT_FOUND_FOR_HOLDING.getCode(), List.of(id.toString()), jobExecutionId);
        log.error(format(ERROR_MESSAGE_TENANT_NOT_FOUND_FOR_HOLDING.getDescription(), id));
      }
    });
    return idsMap;
  }

  private Map<UUID, String> getHoldingIdsTenant(Set<UUID> ids, String centralTenantId) {
    log.info("getHoldingIdsTenant ids: {}", ids);
    Map<UUID, String> idsMap = new HashMap<>();
    var availableTenants = consortiaService.getAffiliatedTenants(folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString());
    ids.forEach(id -> {
      var curTenant = consortiumSearchClient.getHoldingsById(id.toString()).getTenantId();
      if (nonNull(curTenant) && (availableTenants.contains(curTenant) || curTenant.equals(centralTenantId))) {
        idsMap.put(id, curTenant);
      }
    });
    return idsMap;
  }

  private Map<UUID, String> getInstanceIdsTenant(List<HoldingsRecordEntity> holdings, String centralTenantId) {
    log.info("getInstanceIdsTenant ids: {}", holdings);
    Map<UUID, String> idsMap = new HashMap<>();
    var availableTenants = consortiaService.getAffiliatedTenants(folioExecutionContext.getTenantId(), folioExecutionContext.getUserId().toString());
    holdings.forEach(hold -> {
      var curTenant = consortiumSearchClient.getHoldingsById(hold.getId().toString()).getTenantId();
      if (nonNull(curTenant) && (availableTenants.contains(curTenant) || curTenant.equals(centralTenantId))) {
        idsMap.put(hold.getInstanceId(), curTenant);
      }
    });
    return idsMap;
  }

  private void fillOutMarcRecords(Map<UUID, JSONObject> holdingsWithInstanceAndItems, UUID jobExecutionId, List<String> marcRecords,
                                  GeneratedMarcResult result, List<Rule> rules) {
    log.info("holdingsWithInstanceAndItems: {}", holdingsWithInstanceAndItems);
    var centralTenantId = consortiaService.getCentralTenantId(folioExecutionContext.getTenantId());
    if (nonNull(centralTenantId) && centralTenantId.equals(folioExecutionContext.getTenantId())) {
      fillOutFromCentralTenant(holdingsWithInstanceAndItems, jobExecutionId, centralTenantId, marcRecords, result, rules);
    } else {
      for (var jsonObject : holdingsWithInstanceAndItems.values()) {
        try {
          ReferenceDataWrapper referenceDataWrapper = referenceDataProvider.getReference(folioExecutionContext.getTenantId());
          var marc = mapToMarc(jsonObject, rules, referenceDataWrapper);
          marcRecords.add(marc);
        } catch (MarcException e) {
          handleMarcException(jsonObject, result, e, jobExecutionId);
        }
      }
    }
  }

  private void fillOutFromCentralTenant(Map<UUID, JSONObject> holdingsWithInstanceAndItems, UUID jobExecutionId, String centralTenantId, List<String> marcRecords,
                                        GeneratedMarcResult result, List<Rule> rules) {
    var idsTenant = getHoldingIdsTenant(holdingsWithInstanceAndItems.keySet(), centralTenantId);
    log.info("idsTenant: {}", idsTenant);
    for (Map.Entry<UUID, JSONObject> uuidJson : holdingsWithInstanceAndItems.entrySet()) {
      log.info("uuidJson: {}, {}", uuidJson, idsTenant.get(uuidJson.getKey()));
      var tenantId = idsTenant.get(uuidJson.getKey());
      try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {
        ReferenceDataWrapper referenceDataWrapper = referenceDataProvider.getReference(tenantId);
        var marc = mapToMarc(uuidJson.getValue(), rules, referenceDataWrapper);
        log.info("marc: {}", marc);
        marcRecords.add(marc);
      } catch (MarcException e) {
        handleMarcException(uuidJson.getValue(), result, e, jobExecutionId);
      }
    }
  }

  private void handleMarcException(JSONObject jsonObject, GeneratedMarcResult result, MarcException e, UUID jobExecutionId) {
    var holdingsArray = (JSONArray) jsonObject.get(HOLDINGS_KEY);
    var holdingsJsonObject = (JSONObject) holdingsArray.get(0);
    var uuid = holdingsJsonObject.getAsString(ID_KEY);
    result.addIdToFailed(UUID.fromString(uuid));
    var errorMessage = format("%s for holding %s", e.getMessage(), uuid);
    errorLogService.saveGeneralErrorWithMessageValues(ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode(), List.of(errorMessage), jobExecutionId);
    log.error(" getGeneratedMarc::  exception to convert in marc: {}", errorMessage);
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

  @Override
  public MarcRecordEntity getMarcRecord(UUID externalId) {
    throw new UnsupportedOperationException("The functionality is not required for holdings.");
  }

  @Override
  public MappingProfile getDefaultMappingProfile() {
    throw new UnsupportedOperationException("The functionality is not required for holdings.");
  }
}
