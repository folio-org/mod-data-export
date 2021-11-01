package org.folio.service.loader;

import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class SrsLoadResult {
  private List<JsonObject> underlyingMarcRecords = new ArrayList<>();
  private List<String> idsWithoutSrs = new ArrayList<>();

  public List<JsonObject> getUnderlyingMarcRecords() {
    return underlyingMarcRecords;
  }

  public void setUnderlyingMarcRecords(List<JsonObject> underlyingMarcRecords) {
    this.underlyingMarcRecords = underlyingMarcRecords;
  }

  public List<String> getIdsWithoutSrs() {
    return idsWithoutSrs;
  }

  public void setIdsWithoutSrs(List<String> idsWithoutSrs) {
    this.idsWithoutSrs = idsWithoutSrs;
  }
}
