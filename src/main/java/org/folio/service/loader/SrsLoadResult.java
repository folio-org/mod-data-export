package org.folio.service.loader;

import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class SrsLoadResult {
  private List<JsonObject> underlyingMarcRecords = new ArrayList<>();
  private List<String> instanceIdsWithoutSrs = new ArrayList<>();

  public List<JsonObject> getUnderlyingMarcRecords() {
    return underlyingMarcRecords;
  }

  public void setUnderlyingMarcRecords(List<JsonObject> underlyingMarcRecords) {
    this.underlyingMarcRecords = underlyingMarcRecords;
  }

  public List<String> getInstanceIdsWithoutSrs() {
    return instanceIdsWithoutSrs;
  }

  public void setInstanceIdsWithoutSrs(List<String> instanceIdsWithoutSrs) {
    this.instanceIdsWithoutSrs = instanceIdsWithoutSrs;
  }
}
