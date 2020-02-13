package org.folio.service.loader;

import java.util.Collection;

import io.vertx.core.json.JsonObject;

public class SrsLoadResult {

  private Collection<String> underlyingMarcRecords;

  private Collection<String> singleInstanceIdentifiers;

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
