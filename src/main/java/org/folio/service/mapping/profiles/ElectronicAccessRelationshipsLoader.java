package org.folio.service.mapping.profiles;

import io.vertx.core.json.JsonObject;
import org.folio.clients.InventoryClient;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class ElectronicAccessRelationshipsLoader implements SettingsLoader {

  @Autowired
  private InventoryClient inventoryClient;

  @Override
  public Map<String, JsonObject> load(OkapiConnectionParams okapiConnectionParams) {
    return inventoryClient.getElectronicAccessRelationships(okapiConnectionParams);
  }

}
