package org.folio.service.mapping.processor.rule;

import java.util.List;

public class Rule {
  private String tag;
  private String description;
  private List<DataSource> dataSources;

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
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
