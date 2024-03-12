package org.folio.service.logs;

import com.github.javaparser.utils.Pair;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.AffectedRecord;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

import static org.folio.rest.jaxrs.model.AffectedRecord.RecordType.ITEM;

@Component
public class AffectedRecordItemBuilder implements AffectedRecordBuilder {
  private static final String ITEMS = "items";
  private static final String HOLDINGS = "holdings";
  private static final String INSTANCE_ID = "instanceId";
  private static final String ID_KEY = "id";
  private static final String SLASH_SEPARATOR = "/";

  @Autowired
  ConfigurationsClient configurationsClient;

  @Autowired
  AffectedRecordHoldingBuilder affectedRecordHoldingBuilder;

  @Override
  public AffectedRecord build(JsonObject record, String jobExecutionId, String recordId, boolean isLinkCreationRequired,
                              OkapiConnectionParams params) {
    AffectedRecord itemRecord = new AffectedRecord();
    Optional<Pair<JsonObject, JsonObject>> itemAndRelatedHolding =
      getItemAndRelatedHolding(record.getJsonArray(HOLDINGS), recordId);
    if (itemAndRelatedHolding.isPresent()) {
      itemRecord = getAffectedRecordFromJson(itemAndRelatedHolding.get().a, ITEM);
      JsonObject holdingsRecord = itemAndRelatedHolding.get().b;
      itemRecord.setAffectedRecords(Collections.singletonList(affectedRecordHoldingBuilder
        .build(record, jobExecutionId, holdingsRecord.getString(ID_KEY), false, params)));
      if (isLinkCreationRequired) {
        String linkToInventoryRecord = configurationsClient
          .getInventoryRecordLink(getIdsUrlPart(holdingsRecord, recordId), jobExecutionId, params);
        if (StringUtils.isNotEmpty(linkToInventoryRecord)) {
          itemRecord.setInventoryRecordLink(linkToInventoryRecord);
        }
      }
    }
    return itemRecord;
  }

  private String getIdsUrlPart(JsonObject holding, String itemId) {
    return StringUtils.joinWith(
      SLASH_SEPARATOR, holding.getString(INSTANCE_ID), holding.getString(ID_KEY), itemId);
  }

  private Optional<Pair<JsonObject, JsonObject>> getItemAndRelatedHolding(
    JsonArray holdings, String itemRecordId) {
    for (Object recordObj : holdings) {
      JsonObject holding = JsonObject.mapFrom(recordObj);
      JsonArray items = holding.getJsonArray(ITEMS);
      for (Object itemObj : items) {
        JsonObject item = JsonObject.mapFrom(itemObj);
        if (itemRecordId.equals(item.getString(ID_KEY))) {
          return Optional.of(new Pair<>(item, holding));
        }
      }
    }
    return Optional.empty();
  }
}
