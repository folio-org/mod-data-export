package org.folio.service.mapping.processor.rule;

import io.vertx.core.json.JsonObject;

public class Translation {
  private String function;
  private JsonObject parameters;

  public String getFunction() {
    return function;
  }

  public void setFunction(String function) {
    this.function = function;
  }

  public JsonObject getParameters() {
    return parameters;
  }

  public void setParameters(JsonObject parameters) {
    this.parameters = parameters;
  }
}
