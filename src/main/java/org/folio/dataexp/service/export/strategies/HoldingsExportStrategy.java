package org.folio.dataexp.service.export.strategies;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.HoldingsRecordEntityRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.ItemEntityRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.export.strategies.handlers.RuleHandler;
import org.folio.processor.RuleProcessor;
import org.folio.processor.referencedata.ReferenceDataWrapper;
import org.folio.processor.rule.Rule;
import org.folio.reader.EntityReader;
import org.folio.reader.JPathSyntaxEntityReader;
import org.folio.writer.RecordWriter;
import org.folio.writer.impl.MarcRecordWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.folio.dataexp.service.export.Constants.HOLDINGS_KEY;
import static org.folio.dataexp.service.export.Constants.HRID_KEY;
import static org.folio.dataexp.service.export.Constants.ID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_HRID_KEY;
import static org.folio.dataexp.service.export.Constants.INSTANCE_KEY;
import static org.folio.dataexp.service.export.Constants.ITEMS_KEY;

@Log4j2
@Component
@AllArgsConstructor
public class HoldingsExportStrategy extends AbstractExportStrategy {

  private final HoldingsRecordEntityRepository holdingsRecordEntityRepository;
  private final InstanceEntityRepository instanceEntityRepository;
  private final MarcRecordEntityRepository marcRecordEntityRepository;
  private final ItemEntityRepository itemEntityRepository;
  private final RuleFactory ruleFactory;
  private final RuleProcessor ruleProcessor;
  private final RuleHandler ruleHandler;

  @Override
  public List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds, MappingProfile mappingProfile) {
    if (mappingProfile.getDefault()) return marcRecordEntityRepository.findByExternalIdIn(externalIds);
    return new ArrayList<>();
  }

  @Override
  public GeneratedMarcResult getGeneratedMarc(Set<UUID> holdingsIds, MappingProfile mappingProfile) {
    var result = new GeneratedMarcResult();
    var holdingsWithInstanceAndItems = getHoldingsWithInstanceAndItems(holdingsIds, result, mappingProfile);
    var rules = ruleFactory.getRules(mappingProfile, null);
    var marcRecords = holdingsWithInstanceAndItems.stream().map(h -> mapToMarc(h, new ArrayList<>(rules))).toList();
    result.setMarcRecords(marcRecords);
    return result;
  }

  protected List<JSONObject> getHoldingsWithInstanceAndItems(Set<UUID> holdingsIds, GeneratedMarcResult result, MappingProfile mappingProfile) {
    var holdings = holdingsRecordEntityRepository.findByIdIn(holdingsIds);
    var instancesIds = holdings.stream().map(HoldingsRecordEntity::getInstanceId).collect(Collectors.toSet());
    var instances = instanceEntityRepository.findByIdIn(instancesIds);
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
        result.addIdToFailed(holdingsId);
      });
    return holdingsWithInstanceAndItems;
  }

  private String mapToMarc(JSONObject jsonObject, List<Rule> rules) {
    rules = ruleHandler.preHandle(jsonObject, rules);
    EntityReader entityReader = new JPathSyntaxEntityReader(jsonObject.toJSONString());
    RecordWriter recordWriter = new MarcRecordWriter();
    ReferenceDataWrapper referenceDataWrapper = null;
    return ruleProcessor.process(entityReader, recordWriter, referenceDataWrapper, rules, (translationException -> {
      var holdingsArray = (JSONArray) jsonObject.get(HOLDINGS_KEY);
      var holdingsJsonObject = (JSONObject) holdingsArray.get(0);
      log.warn("mapToSrs:: exception: {} for holding {}", translationException.getCause().getMessage(), holdingsJsonObject.get(ID_KEY));
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
