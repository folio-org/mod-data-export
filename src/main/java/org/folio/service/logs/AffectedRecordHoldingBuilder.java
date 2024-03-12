package org.folio.service.logs;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.AffectedRecord;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import static org.folio.rest.jaxrs.model.AffectedRecord.RecordType.HOLDINGS;

@Component
public class AffectedRecordHoldingBuilder implements AffectedRecordBuilder {

  private static final String INSTANCE_ID = "instanceId";
  private static final String ID_KEY = "id";
  private static final String SLASH_SEPARATOR = "/";

  @Autowired
  private ConfigurationsClient configurationsClient;

  @Autowired
  private AffectedRecordInstanceBuilder affectedRecordInstanceBuilder;

  @Override
  public AffectedRecord build(JsonObject record, String jobExecutionId, String recordId, boolean isLinkCreationRequired,
                              OkapiConnectionParams params) {
    AffectedRecord holdingRecord = new AffectedRecord();
    JsonArray holdings = record.getJsonArray(HOLDINGS.toString().toLowerCase());
    Optional<JsonObject> holdingJson = getRecordFromArrayById(holdings, recordId);
    if (holdingJson.isPresent()) {
      holdingRecord = getAffectedRecordFromJson(holdingJson.get(), HOLDINGS);
      String instanceId = holdingJson.get().getString(INSTANCE_ID);
      holdingRecord.setAffectedRecords(Collections.singletonList(affectedRecordInstanceBuilder
        .build(record, jobExecutionId, instanceId, false, params)));
      if (isLinkCreationRequired) {
        String linkToInventoryRecord = configurationsClient
          .getInventoryRecordLink(getIdsUrlPart(recordId, instanceId), jobExecutionId, params);
        if (StringUtils.isNotEmpty(linkToInventoryRecord)) {
          holdingRecord.setInventoryRecordLink(linkToInventoryRecord);
        }
      }
    }
    return holdingRecord;
  }

  private String getIdsUrlPart(String holdingId, String instanceId) {
    return StringUtils.joinWith(SLASH_SEPARATOR, instanceId, holdingId);
  }

  private Optional<JsonObject> getRecordFromArrayById(JsonArray records, String id) {
    if (Objects.nonNull(records)) {
      for (Object record : records) {
        JsonObject recordJson = JsonObject.mapFrom(record);
        if (id.equals(recordJson.getString(ID_KEY))) {
          return Optional.of(recordJson);
        }
      }
    }
    return Optional.empty();
  }

}
