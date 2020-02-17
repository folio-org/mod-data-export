package org.folio.service.loader;

import java.util.ArrayList;
import java.util.Collection;

public class SrsLoadResult {

  private Collection<String> underlyingMarcRecords = new ArrayList<>();

  private Collection<String> singleInstanceIdentifiers = new ArrayList<>();;

  public Collection<String> getUnderlyingMarcRecords() {
    return underlyingMarcRecords;
  }

  public void setUnderlyingMarcRecords(Collection<String> underlyingMarcRecords) {
    this.underlyingMarcRecords = underlyingMarcRecords;
  }

  public Collection<String> getSingleInstanceIdentifiers() {
    return singleInstanceIdentifiers;
  }

  public void setSingleInstanceIdentifiers(Collection<String> singleInstanceIdentifiers) {
    this.singleInstanceIdentifiers = singleInstanceIdentifiers;
  }
}
