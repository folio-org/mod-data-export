package org.folio.service.mapping.processor.rule;

import java.util.List;

public class Rule {
  private String field;
  private String description;
  private List<DataSource> dataSources;

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<DataSource> getDataSources() {
    return dataSources;
  }

  public void setDataSources(List<DataSource> dataSources) {
    this.dataSources = dataSources;
  }
}
