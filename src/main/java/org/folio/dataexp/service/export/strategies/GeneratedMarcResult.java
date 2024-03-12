package org.folio.dataexp.service.export.strategies;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class GeneratedMarcResult {
  private List<String> marcRecords = new ArrayList<>();
  private final List<UUID> failedIds = new ArrayList<>();
  private final List<UUID> notExistIds = new ArrayList<>();

  public void setMarcRecords(List<String> marcRecords) {
    this.marcRecords = marcRecords;
  }

  public void addIdToFailed(UUID id) {
    failedIds.add(id);
  }

  public void addIdToNotExist(UUID id) {
    notExistIds.add(id);
  }
}
