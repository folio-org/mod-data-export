package org.folio.domain;

import java.util.Collection;

public class Batch<T> {
  private final Collection<T> data;

  public Batch(Collection<T> data) {
    this.data = data;
  }

  public Collection<T> getData() {
    return data;
  }
}
