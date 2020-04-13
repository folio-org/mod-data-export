package org.folio.service.mapping.processor.rule;

import io.vertx.core.json.JsonObject;

public class DataSource {
  private String tag;
  private String subField;
  private String indicator;
  private String from;
  private Translation translation;

  public DataSource(String tag, JsonObject dataSource) {
    this.tag = tag;
    this.from = dataSource.getString("from");
    this.subField = dataSource.getString("subField");
    this.indicator = dataSource.getString("indicator");
    if (dataSource.containsKey("translation")) {
      this.translation = dataSource.getJsonObject("translation").mapTo(Translation.class);
    }
  }

  public String getTag() {
    return this.tag;
  }

  public String getSubField() {
    return subField;
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
    return this.subField != null;
  }

  public boolean isIndicatorSource() {
    return this.indicator != null;
  }
}
