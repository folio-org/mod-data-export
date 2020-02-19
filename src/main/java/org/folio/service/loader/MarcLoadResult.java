package org.folio.service.loader;


import java.util.ArrayList;
import java.util.List;

public class MarcLoadResult {
  private List<String> srsMarcRecords;
  private List<String> singleInstanceIdentifiers;

  public MarcLoadResult() {
    this.srsMarcRecords = new ArrayList<>();
    this.singleInstanceIdentifiers = new ArrayList<>();
  }

  public List<String> getSrsMarcRecords() {
    return srsMarcRecords;
  }

  public List<String> getSingleInstanceIdentifiers() {
    return singleInstanceIdentifiers;
  }

  public void setSrsMarcRecords(List<String> srsMarcRecords) {
    this.srsMarcRecords = srsMarcRecords;
  }

  public void setSingleInstanceIdentifiers(List<String> singleInstanceIdentifiers) {
    this.singleInstanceIdentifiers = singleInstanceIdentifiers;
  }
}
