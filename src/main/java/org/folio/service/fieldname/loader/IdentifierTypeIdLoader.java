package org.folio.service.fieldname.loader;

import io.vertx.core.json.JsonObject;
import org.folio.clients.InventoryClient;
import org.folio.util.OkapiConnectionParams;

import java.util.Map;

public class IdentifierTypeIdLoader implements ReferenceDataLoader {

  private InventoryClient inventoryClient;

  public IdentifierTypeIdLoader() {
    this.inventoryClient = new InventoryClient();
  }

  @Override
  public Map<String, JsonObject> load(OkapiConnectionParams okapiConnectionParams) {
    return inventoryClient.getIdentifierTypes(okapiConnectionParams);
  }
}
