package org.folio.dataexp.service.export.strategies;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.HoldingsRecordDeletedEntity;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.HoldingsRecordEntityDeletedRepository;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.service.transformationfields.ReferenceDataProvider;
import org.folio.processor.RuleProcessor;
import org.folio.processor.referencedata.ReferenceDataWrapper;
import org.folio.processor.rule.Rule;
import org.folio.reader.EntityReader;
import org.folio.reader.JPathSyntaxEntityReader;
import org.folio.writer.RecordWriter;
import org.folio.writer.impl.MarcRecordWriter;
import org.marc4j.MarcException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.folio.dataexp.service.export.Constants.HOLDINGS_KEY;
import static org.folio.dataexp.service.export.Constants.HRID_KEY;
import static org.folio.dataexp.service.export.Constants.ID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_HRID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_KEY;
import static org.folio.dataexp.service.export.Constants.ITEMS_KEY;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC;

@Log4j2
@Component
@AllArgsConstructor
public class HoldingsExportStrategy extends AbstractExportStrategy {
  private static final String HOLDING_MARC_TYPE = "MARC_HOLDING";

  private final HoldingsRecordEntityRepository holdingsRecordEntityRepository;
  private final HoldingsRecordEntityDeletedRepository holdingsRecordEntityDeletedRepository;
  private final InstanceEntityRepository instanceEntityRepository;
  private final MarcRecordEntityRepository marcRecordEntityRepository;
  private final ItemEntityRepository itemEntityRepository;
  private final RuleFactory ruleFactory;
  private final RuleProcessor ruleProcessor;
  private final RuleHandler ruleHandler;
  private final ReferenceDataProvider referenceDataProvider;
  private final ErrorLogService errorLogService;

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds, MappingProfile mappingProfile, ExportRequest exportRequest) {
    if (Boolean.TRUE.equals(mappingProfile.getDefault())) {
      if (exportRequest.getAll()) {
        if (exportRequest.getDeletedRecords()) {
          return marcRecordEntityRepository.findByExternalIdInAndRecordTypeIsAndSuppressDiscoveryIs(externalIds, HOLDING_MARC_TYPE,
              exportRequest.getSuppressedFromDiscovery());
        }
        return marcRecordEntityRepository.findByExternalIdInAndRecordTypeIsAndStateIsAndLeaderRecordStatusNotAndSuppressDiscoveryIs(
            externalIds, HOLDING_MARC_TYPE, "ACTUAL", 'd', exportRequest.getSuppressedFromDiscovery());
      }
      return marcRecordEntityRepository.findByExternalIdInAndRecordTypeIsAndStateIsAndLeaderRecordStatusNot(externalIds,
          HOLDING_MARC_TYPE, "ACTUAL", 'd');
    }
    return new ArrayList<>();
  }

  @Override
  public GeneratedMarcResult getGeneratedMarc(Set<UUID> holdingsIds, MappingProfile mappingProfile, ExportRequest exportRequest,
      boolean lastSlice, boolean lastExport, UUID jobExecutionId, ExportStrategyStatistic exportStatistic) {
    var result = new GeneratedMarcResult();
    var holdingsWithInstanceAndItems = getHoldingsWithInstanceAndItems(holdingsIds, result, mappingProfile, exportRequest, lastSlice,
        lastExport);
    var rules = ruleFactory.getRules(mappingProfile);
    ReferenceDataWrapper referenceDataWrapper = referenceDataProvider.getReference();
    var marcRecords = holdingsWithInstanceAndItems.stream()
      .filter(h -> !h.isEmpty()).map(h -> mapToMarc(h, new ArrayList<>(rules), referenceDataWrapper, jobExecutionId,
        exportStatistic)).toList();
    result.setMarcRecords(marcRecords);
    return result;
  }

  @Override
  public Optional<String> getIdentifierMessage(UUID id) {
    var holdings = holdingsRecordEntityRepository.findByIdIn(Set.of(id));
    if (holdings.isEmpty()) return Optional.empty();
    var jsonObject =  getAsJsonObject(holdings.get(0).getJsonb());
    if (jsonObject.isPresent()) {
      var hrid = jsonObject.get().getAsString(HRID_KEY);
      return Optional.of("Holding with hrid : " + hrid);
    }
    return Optional.empty();
  }

  protected List<JSONObject> getHoldingsWithInstanceAndItems(Set<UUID> holdingsIds, GeneratedMarcResult result,
      MappingProfile mappingProfile, ExportRequest exportRequest, boolean lastSlice, boolean lastExport) {
    var holdings = holdingsRecordEntityRepository.findByIdIn(holdingsIds);
    var instancesIds = holdings.stream().map(HoldingsRecordEntity::getInstanceId).collect(Collectors.toSet());
    if (exportRequest.getAll() && exportRequest.getDeletedRecords() && lastSlice && lastExport) {
      List<HoldingsRecordDeletedEntity> holdingsDeleted;
      if (exportRequest.getSuppressedFromDiscovery()) {
        holdingsDeleted = holdingsRecordEntityDeletedRepository.findAll();
      } else {
        holdingsDeleted = holdingsRecordEntityDeletedRepository.findAllDeletedWhenSkipDiscoverySuppressed();
      }
      var holdingsDeletedToHoldingsEntities = holdingsDeletedToHoldingsEntities(holdingsDeleted);
      var instanceIdsDeleted = holdingsDeletedToHoldingsEntities.stream().map(HoldingsRecordEntity::getInstanceId).collect(Collectors.toSet());
      holdings.addAll(holdingsDeletedToHoldingsEntities);
      instancesIds.addAll(instanceIdsDeleted);
    }
    var instances = instanceEntityRepository.findByIdIn(instancesIds);
    entityManager.clear();
    List<JSONObject> holdingsWithInstanceAndItems = new ArrayList<>();
    var existHoldingsIds = new HashSet<UUID>();
    for (var holding : holdings) {
      existHoldingsIds.add(holding.getId());
      var holdingJsonOpt = getAsJsonObject(holding.getJsonb());
      if (holdingJsonOpt.isEmpty()) {
        log.error("getHoldingsWithInstanceAndItems:: Error converting to json holding by id {}", holding.getId());
        result.addIdToFailed(holding.getId());
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
        result.addIdToNotExist(holdingsId);
        result.addIdToFailed(holdingsId);
      });
    return holdingsWithInstanceAndItems;
  }

  private String mapToMarc(JSONObject jsonObject, List<Rule> rules, ReferenceDataWrapper referenceDataWrapper,
                           UUID jobExecutionId, ExportStrategyStatistic exportStatistic) {
    rules = ruleHandler.preHandle(jsonObject, rules);
    EntityReader entityReader = new JPathSyntaxEntityReader(jsonObject.toJSONString());
    RecordWriter recordWriter = new MarcRecordWriter();
    try {
      return ruleProcessor.process(entityReader, recordWriter, referenceDataWrapper, rules, (translationException -> {
        var holdingsArray = (JSONArray) jsonObject.get(HOLDINGS_KEY);
        var holdingsJsonObject = (JSONObject) holdingsArray.get(0);
        log.warn("mapToSrs:: exception: {} for holding {}", translationException.getCause().getMessage(), holdingsJsonObject.get(ID_KEY));
      }));
    } catch (MarcException e) {
      errorLogService.saveWithAffectedRecord(jsonObject, ERROR_MESSAGE_JSON_CANNOT_BE_CONVERTED_TO_MARC.getCode(), jobExecutionId, e);
      log.error(e.getMessage());
      exportStatistic.incrementFailed();
      return "";
    }
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

  private List<HoldingsRecordEntity> holdingsDeletedToHoldingsEntities(List<HoldingsRecordDeletedEntity> holdingsDeleted) {
    return holdingsDeleted.stream()
        .map(hold -> new HoldingsRecordEntity().withId(UUID.fromString(getAsJsonObject(getAsJsonObject(hold.getJsonb()).get()
                .getAsString("record")).get()
                .getAsString("id")))
            .withJsonb(getAsJsonObject(hold.getJsonb()).get()
                .getAsString("record"))
            .withInstanceId(UUID.fromString(getAsJsonObject(getAsJsonObject(hold.getJsonb()).get()
                .getAsString("record")).get()
                .getAsString("instanceId"))))
        .collect(Collectors.toList());
  }
}
