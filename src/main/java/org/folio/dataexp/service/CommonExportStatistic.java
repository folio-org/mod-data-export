package org.folio.dataexp.service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import org.folio.dataexp.service.export.strategies.ExportedRecordsListener;

/** Holds statistics and state for a common export operation, such as invalid or duplicate UUIDs. */
public class CommonExportStatistic {
  /** Indicates if reading the input file failed. */
  @Getter @Setter private boolean isFailedToReadInputFile = true;

  /** Set of UUIDs that do not exist. */
  @Getter
  private final Set<String> notExistUuid = Collections.synchronizedSet(new LinkedHashSet<>());

  /** List of IDs with invalid UUID format. */
  @Getter private final List<String> invalidUuidFormat = new LinkedList<>();

  /** Amount of duplicated UUIDs. */
  private AtomicInteger duplicatedUuidAmount = new AtomicInteger(0);

  public int getDuplicatedUuidAmount() {
    return duplicatedUuidAmount.get();
  }

  /** Listener for exported records. */
  @Getter @Setter private ExportedRecordsListener exportedRecordsListener;

  /**
   * Adds a list of UUIDs to the set of non-existent UUIDs.
   *
   * @param ids List of UUIDs to add.
   */
  public void addToNotExistUuidAll(List<UUID> ids) {
    var idsToString = ids.stream().map(UUID::toString).toList();
    notExistUuid.addAll(idsToString);
  }

  /**
   * Adds an ID to the list of invalid UUID formats.
   *
   * @param id The invalid ID.
   */
  public void addToInvalidUuidFormat(String id) {
    invalidUuidFormat.add(id);
  }

  /**
   * Increments the duplicated UUID amount by a given count.
   *
   * @param count Number to increment by.
   */
  public void incrementDuplicatedUuid(int count) {
    duplicatedUuidAmount.addAndGet(count);
  }

  /** Increments the duplicated UUID amount by one. */
  public void incrementDuplicatedUuid() {
    duplicatedUuidAmount.incrementAndGet();
  }
}
