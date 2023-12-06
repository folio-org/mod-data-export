package org.folio.dataexp.service;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CommonExportFails {
  private final Set<UUID> notExistUUID = Collections.synchronizedSet(new LinkedHashSet<>());
  @Getter
  private final Set<String> invalidUUIDFormat = new LinkedHashSet<>();
  @Getter
  private int duplicatedUUIDAmount;

  public void addToNotExistUUIDAll(List<UUID> ids) {
    notExistUUID.addAll(ids);
  }

  public void addToInvalidUUIDFormat(String id) {
    invalidUUIDFormat.add(id);
  }

  public void incrementDuplicatedUUID(int count) {
    this.duplicatedUUIDAmount = this.duplicatedUUIDAmount + count;
  }

  public void incrementDuplicatedUUID() {
    this.duplicatedUUIDAmount = this.duplicatedUUIDAmount + 1;
  }
}
