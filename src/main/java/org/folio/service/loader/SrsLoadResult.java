package org.folio.service.loader;

import java.util.ArrayList;
import java.util.List;

public class SrsLoadResult {
  private List<String> underlyingMarcRecords = new ArrayList<>();
  private List<String> instanceIdsWithoutSrs = new ArrayList<>();

  public List<String> getUnderlyingMarcRecords() {
    return underlyingMarcRecords;
  }

  public void setUnderlyingMarcRecords(List<String> underlyingMarcRecords) {
    this.underlyingMarcRecords = underlyingMarcRecords;
  }

  public List<String> getInstanceIdsWithoutSrs() {
    return instanceIdsWithoutSrs;
  }

  public void setInstanceIdsWithoutSrs(List<String> instanceIdsWithoutSrs) {
    this.instanceIdsWithoutSrs = instanceIdsWithoutSrs;
  }
}
