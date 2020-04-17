package org.folio.service.mapping.processor.rule;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Rule {
  private List<DataSource> dataSources = new ArrayList<>();

  public Rule(JsonObject rule) {
    String tag = rule.getString("tag");
    JsonArray dataSources = rule.getJsonArray("dataSources");
    if (dataSources.isEmpty()) {
      throw new IllegalArgumentException(String.format("The given rule does not have data source, rule : %s", rule));
    } else {
      dataSources.forEach(item -> this.dataSources.add(new DataSource(tag, (JsonObject) item)));
    }
  }

  public List<DataSource> getDataSources() {
    return this.dataSources;
  }
}
