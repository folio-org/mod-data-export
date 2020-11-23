package org.folio.service.logs;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.AffectedRecord;
import org.folio.util.OkapiConnectionParams;

/**
 * AffectedRecordBuilder interface, contains logic for building affected records
 */
public interface AffectedRecordBuilder {


  /**
   * Returns AffectedRecord
   *
   * @param record                 record used for mapping process
   * @param jobExecutionId         job execution id
   * @param recordId               id of the affected record
   * @param isLinkCreationRequired if true - link to the record will be created, otherwise not
   * @param params                 okapi connection parameters
   * @return {@link AffectedRecord}
   */
  AffectedRecord build(JsonObject record, String jobExecutionId, String recordId, boolean isLinkCreationRequired,
                       OkapiConnectionParams params);

  default AffectedRecord getAffectedRecordFromJson(
    JsonObject recordJson, AffectedRecord.RecordType recordType) {
    AffectedRecord record = new AffectedRecord().withRecordType(recordType);
    if (StringUtils.isNotBlank(recordJson.getString("hrid"))) {
      record.setHrid(recordJson.getString("hrid"));
    }
    if (StringUtils.isNotBlank(recordJson.getString("id"))) {
      record.setId(recordJson.getString("id"));
    }
    if (StringUtils.isNotBlank(recordJson.getString("title"))) {
      record.setTitle(recordJson.getString("title"));
    }
    return record;
  }

}
