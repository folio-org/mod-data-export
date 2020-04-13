package org.folio.service.mapping.processor.rule;

import io.vertx.core.json.JsonObject;

public class DataSource {
  private String tag;
  private String subfield;
  private String indicator;
  private String from;
  private Translation translation;

  public DataSource(String tag, JsonObject dataSource) {
    this.tag = tag;
    this.from = dataSource.getString("from");
    this.subfield = dataSource.getString("subfield");
    this.indicator = dataSource.getString("indicator");
    if (dataSource.containsKey("translation")) {
      this.translation = new Translation(dataSource.getJsonObject("translation"));
    }
  }

  public String getTag() {
    return this.tag;
  }

  public String getSubfield() {
    return subfield;
  }

  public String getIndicator() {
    return indicator;
  }

  public String getFrom() {
    return from;
  }

  public Translation getTranslation() {
    return translation;
  }

  public boolean isSubFieldSource() {
    return this.subfield != null;
  }

  public boolean isIndicatorSource() {
    return this.indicator != null;
  }
}
