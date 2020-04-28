package org.folio.service.mapping.settings;

import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.list.UnmodifiableList;

import java.util.List;

public class Settings {
  private UnmodifiableList<JsonObject> natureOfContentTerms;

  public void addNatureOfContentTerms(List<JsonObject> natureOfContentTerms) {
    this.natureOfContentTerms = new UnmodifiableList<>(natureOfContentTerms);
  }

  public UnmodifiableList<JsonObject> getNatureOfContentTerms() {
    return natureOfContentTerms;
  }
}
