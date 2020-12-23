package org.folio.service.mapping.converter;

import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.service.mapping.MappingService;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.folio.util.ExternalPathResolver.INSTANCE;

@Service
public class InventoryRecordConverterService extends RecordConverter {
  @Autowired
  private MappingService mappingService;
  private static final String ID_FIELD = "id";
  private static final String HR_ID_FIELD = "hrid";

  public List<String> transformInventoryRecords(List<JsonObject> instances, String jobExecutionId, MappingProfile mappingProfile,
      OkapiConnectionParams params) {
    instances = appendHoldingsAndItems(instances, mappingProfile, jobExecutionId, params);
    return mappingService.map(instances, mappingProfile, jobExecutionId, params);
  }

  /**
   * For Each instance UUID fetches all the holdings and also items for each holding and appends it to a single record
   *
   * @param instances      list of instance objects
   * @param mappingProfile {@link MappingProfile}
   * @param jobExecutionId job execution id
   * @param params         okapi headers and connection parameters
   */
  protected List<JsonObject> appendHoldingsAndItems(List<JsonObject> instances, MappingProfile mappingProfile,
                                                    String jobExecutionId, OkapiConnectionParams params) {
    List<JsonObject> instancesWithHoldingsAndItems = new ArrayList<>();
    for (JsonObject instance : instances) {
      JsonObject instanceWithHoldingsAndItems = new JsonObject();
      instanceWithHoldingsAndItems.put(INSTANCE, instance);
      fetchHoldingsAndItems(mappingProfile, params, instance.getString(ID_FIELD), instance.getString(HR_ID_FIELD), instanceWithHoldingsAndItems, jobExecutionId);
      instancesWithHoldingsAndItems.add(instanceWithHoldingsAndItems);
    }
    return instancesWithHoldingsAndItems;

  }

}
