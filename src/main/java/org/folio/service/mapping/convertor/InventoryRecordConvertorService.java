package org.folio.service.mapping.convertor;

import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.service.mapping.MappingService;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InventoryRecordConvertorService extends RecordConvertor {
  @Autowired
  private MappingService mappingService;

  public List<String> transformInventoryRecords(List<JsonObject> instances, String jobExecutionId, MappingProfile mappingProfile,
      OkapiConnectionParams params) {
    instances = appendHoldingsAndItems(instances, mappingProfile, params);
    return mappingService.map(instances, mappingProfile, jobExecutionId, params);
  }

  /**
   * For Each instance UUID fetches all the holdings and also items for each holding and appends it to a single record
   *
   * @param instances list of instance objects
   * @param params
   */
  protected List<JsonObject> appendHoldingsAndItems(List<JsonObject> instances, MappingProfile mappingProfile,
      OkapiConnectionParams params) {
    List<JsonObject> instancesWithHoldingsAndItems = new ArrayList<>();
    for (JsonObject instance : instances) {
      JsonObject instanceWithHoldingsAndItems = new JsonObject();
      instanceWithHoldingsAndItems.put("instance", instance);
      fetchHoldingsAndItems(mappingProfile, params, instance.getString("id"), instanceWithHoldingsAndItems);
      instancesWithHoldingsAndItems.add(instanceWithHoldingsAndItems);
    }
    return instancesWithHoldingsAndItems;

  }

}