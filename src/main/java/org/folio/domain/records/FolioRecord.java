package org.folio.domain.records;


public abstract class FolioRecord {

  private final Object data;

  protected FolioRecord(Object data) {
    this.data = data;
  }
}
