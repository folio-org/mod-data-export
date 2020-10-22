package org.folio.service.loader;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InventoryLoadResult {
  private List<JsonObject> instances = new ArrayList<>();
  private Collection<String> notFoundInstancesUUIDs = new ArrayList<>();

  public List<JsonObject> getInstances() {
    return instances;
  }

  public void setInstances(List<JsonObject> instances) {
    this.instances = instances;
  }

  public Collection<String> getNotFoundInstancesUUIDs() {
    return notFoundInstancesUUIDs;
  }

  public void setNotFoundInstancesUUIDs(Collection<String> notFoundInstancesUUIDs) {
    this.notFoundInstancesUUIDs = notFoundInstancesUUIDs;
  }
}
