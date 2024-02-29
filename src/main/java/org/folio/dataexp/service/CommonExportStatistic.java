package org.folio.dataexp.service;

import lombok.Getter;
import lombok.Setter;
import org.folio.dataexp.service.export.strategies.ExportStrategyStatisticListener;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CommonExportStatistic {
  @Getter
  @Setter
  private boolean isFailedToReadInputFile = true;
  @Getter
  private final Set<String> notExistUUID = Collections.synchronizedSet(new LinkedHashSet<>());
  @Getter
  private final List<String> invalidUUIDFormat = new LinkedList<>();
  @Getter
  private int duplicatedUUIDAmount;
  @Getter
  @Setter
  private ExportStrategyStatisticListener exportStrategyStatisticListener;

  public void addToNotExistUUIDAll(List<UUID> ids) {
    var idsToString = ids.stream().map(UUID::toString).toList();
    notExistUUID.addAll(idsToString);
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
