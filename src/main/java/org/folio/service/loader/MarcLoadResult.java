package org.folio.service.loader;


import java.util.ArrayList;
import java.util.List;

public class MarcLoadResult {
  private List<String> srsMarcRecords;
  private List<String> instanceIds;

  public MarcLoadResult() {
    this.srsMarcRecords = new ArrayList<>();
    this.instanceIds = new ArrayList<>();
  }

  public MarcLoadResult(List<String> srsMarcRecords, List<String> instanceIds) {
    this.srsMarcRecords = srsMarcRecords;
    this.instanceIds = instanceIds;
  }

  public List<String> getSrsMarcRecords() {
    return srsMarcRecords;
  }

  public List<String> getInstanceIds() {
    return instanceIds;
  }
}
