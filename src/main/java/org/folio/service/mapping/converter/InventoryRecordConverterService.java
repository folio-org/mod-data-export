package org.folio.service.mapping.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.service.loader.LoadResult;
import org.folio.service.mapping.MappingService;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.folio.util.ExternalPathResolver.INSTANCE;

@Service
public class InventoryRecordConverterService extends RecordConverter {

  private static final String ID_FIELD = "id";
  private static final String HR_ID_FIELD = "hrid";

  @Autowired
  private MappingService mappingService;

  public Pair<List<String>, Integer> transformInstanceRecords(List<JsonObject> instances, String jobExecutionId, MappingProfile mappingProfile,
                                                              OkapiConnectionParams params) {
    instances = appendHoldingsAndItems(instances, mappingProfile, jobExecutionId, params);
    return mappingService.map(instances, mappingProfile, jobExecutionId, params);
  }

  public Pair<List<String>, Integer> transformHoldingRecords(List<JsonObject> holdings, String jobExecutionId, MappingProfile mappingProfile,
                                                             OkapiConnectionParams params) {
    holdings = appendInstancesAndItems(holdings, mappingProfile, jobExecutionId, params);
    return mappingService.map(holdings, mappingProfile, jobExecutionId, params);
  }

  public Pair<List<String>, Integer> transformAuthorityRecords(List<JsonObject> authorities, String jobExecutionId,
                                                       MappingProfile mappingProfile,
                                                             OkapiConnectionParams params) {
    return mappingService.map(authorities, mappingProfile, jobExecutionId, params);
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
      fetchHoldingsAndItems(mappingProfile, params, instance.getString(ID_FIELD), instance.getString(HR_ID_FIELD), RecordType.INSTANCE, instanceWithHoldingsAndItems, jobExecutionId);
      instancesWithHoldingsAndItems.add(instanceWithHoldingsAndItems);
    }
    return instancesWithHoldingsAndItems;
  }

  /**
   * For each holding fetches the related items and instance
   *
   * @param holdings       list of instance objects
   * @param mappingProfile {@link MappingProfile}
   * @param jobExecutionId job execution id
   * @param params         okapi headers and connection parameters
   */
  protected List<JsonObject> appendInstancesAndItems(List<JsonObject> holdings, MappingProfile mappingProfile,
                                                     String jobExecutionId, OkapiConnectionParams params) {
    List<JsonObject> holdingsWithInstanceAndItems = new ArrayList<>();
    LoadResult holdingInstances = fetchInstancesForHoldings(holdings, jobExecutionId, params);
    for (JsonObject holding : holdings) {
      JsonObject holdingWithInstanceAndItems = new JsonObject();
      for (JsonObject instance : holdingInstances.getEntities()) {
        if (instance.getString("id").equals(holding.getString("instanceId"))) {
          holdingWithInstanceAndItems.put(INSTANCE, instance);
          holding.put("instanceHrId", instance.getString("hrid"));
        }
      }
      holdingWithInstanceAndItems.put("holdings", new JsonArray(Collections.singletonList(holding)));
      fetchItemsIfRequired(mappingProfile, params, holding.getString(ID_FIELD), holdingWithInstanceAndItems, jobExecutionId);
      holdingsWithInstanceAndItems.add(holdingWithInstanceAndItems);
    }
    return holdingsWithInstanceAndItems;
  }


}
