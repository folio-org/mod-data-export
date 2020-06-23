package org.folio.service.mapping.processor.rule;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Rule {
  private String id;
  private String field;
  private String description;
  private List<DataSource> dataSources = new ArrayList<>();
  private Metadata metadata;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

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

  public Metadata getMetadata() {
    return this.metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = new Metadata(metadata);
  }
}
