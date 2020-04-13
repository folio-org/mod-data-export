package org.folio.service.mapping.processor.rule;

import io.vertx.core.json.JsonObject;

public class Translation {
  private String function;
  private JsonObject parameters;

  public Translation(JsonObject translation) {
    this.function = translation.getString("function");
    this.parameters = translation.getJsonObject("parameters");
  }

  public String getFunction() {
    return function;
  }

  public JsonObject getParameters() {
    return parameters;
  }
}
