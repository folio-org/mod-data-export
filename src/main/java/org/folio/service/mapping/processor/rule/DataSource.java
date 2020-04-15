package org.folio.service.mapping.processor.rule;

public class DataSource {
  private String subfield;
  private String indicator;
  private String from;
  private Translation translation;

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
