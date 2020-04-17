package org.folio.service.mapping.processor.rule;

import io.vertx.core.json.JsonObject;

import java.util.Map;

public class Translation {
  private String function;
  private JsonObject parameters;

  public void setFunction(String function) {
    this.function = function;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = JsonObject.mapFrom(parameters);
  }

  public String getFunction() {
    return function;
  }

  public JsonObject getParameters() {
    return parameters;
  }
}
