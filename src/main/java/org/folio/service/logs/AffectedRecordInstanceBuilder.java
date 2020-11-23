package org.folio.service.logs;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.clients.ConfigurationsClient;
import org.folio.rest.jaxrs.model.AffectedRecord;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static org.folio.rest.jaxrs.model.AffectedRecord.RecordType.INSTANCE;

@Component
public class AffectedRecordInstanceBuilder implements AffectedRecordBuilder {

  @Autowired
  ConfigurationsClient configurationsClient;

  @Override
  public AffectedRecord build(JsonObject record, String jobExecutionId, String recordId, boolean isLinkCreationRequired,
                              OkapiConnectionParams params) {
    JsonObject instance = record.getJsonObject(INSTANCE.toString().toLowerCase());
    AffectedRecord instanceRecord = new AffectedRecord();
    if (Objects.isNull(instance)) {
      instanceRecord.setRecordType(INSTANCE);
      instanceRecord.setId(recordId);
    } else {
      instanceRecord = getAffectedRecordFromJson(instance, INSTANCE);
      if (isLinkCreationRequired) {
        String linkToInventoryRecord = configurationsClient
          .getInventoryRecordLink(recordId, jobExecutionId, params);
        if (StringUtils.isNotEmpty(linkToInventoryRecord)) {
          instanceRecord.setInventoryRecordLink(linkToInventoryRecord);
        }
      }
    }
    return instanceRecord;
  }
}
