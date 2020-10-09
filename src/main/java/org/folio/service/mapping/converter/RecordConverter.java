package org.folio.service.mapping.converter;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.loader.RecordLoaderService;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;

public class RecordConverter {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup()
      .lookupClass());

  @Autowired
  private RecordLoaderService recordLoaderService;

   protected boolean isTransformationRequired(MappingProfile mappingProfile) {
     List<Transformations> transformations = mappingProfile.getTransformations();
     List<RecordType> recordTypes = mappingProfile.getRecordTypes();
     return isNotEmpty(transformations) && (recordTypes.contains(RecordType.HOLDINGS) || recordTypes.contains(RecordType.ITEM));

   }

  /**
   * Fetches holdings if Transformations specify Record type "HOLDINGS", and
   * also appends items to it if recordtype contains "ITEM"
   *
   * @param mappingProfile
   * @param params
   * @param instanceUUID
   * @param appendHoldingsItems
   */
   protected void fetchHoldingsAndItems(MappingProfile mappingProfile, OkapiConnectionParams params, String instanceUUID,
       JsonObject appendHoldingsItems, String jobExecutionId) {
     if (isTransformationRequired(mappingProfile)) {
       LOGGER.debug("Fetching holdings/items for instance");
       List<JsonObject> holdings = recordLoaderService.getHoldingsForInstance(instanceUUID, jobExecutionId, params);
       appendHoldingsItems.put("holdings", new JsonArray(holdings));
       if (mappingProfile.getRecordTypes().contains(RecordType.ITEM) && CollectionUtils.isNotEmpty(holdings)) {
         List<String> holdingIds = holdings.stream()
           .map(record -> record.getString("id"))
           .collect(Collectors.toList());
         List<JsonObject> items = recordLoaderService.getAllItemsForHolding(holdingIds, jobExecutionId, params);
         appendHoldingsItems.put("items", new JsonArray(items));
       }
     }
   }
}