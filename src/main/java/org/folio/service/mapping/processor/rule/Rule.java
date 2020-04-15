package org.folio.service.mapping.processor.rule;

import java.util.ArrayList;
import java.util.List;


public class Rule {
  private String tag;
  private String description;
  private List<DataSource> dataSources = new ArrayList<>();


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
    return this.dataSources;
  }
}
