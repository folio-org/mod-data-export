package org.folio.dataexp.service.export.strategies;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

/**
 * Holds the result of generated MARC records, including failed and not existing IDs.
 */
@Getter
public class GeneratedMarcResult {
  private UUID jobExecutionId;
  private List<String> marcRecords = new ArrayList<>();
  private final List<UUID> failedIds = new ArrayList<>();
  private final List<UUID> notExistIds = new ArrayList<>();

  /**
   * Constructs a GeneratedMarcResult for the given job execution ID.
   *
   * @param jobExecutionId the job execution ID
   */
  public GeneratedMarcResult(UUID jobExecutionId) {
    this.jobExecutionId = jobExecutionId;
  }

  /**
   * Sets the list of generated MARC records.
   *
   * @param marcRecords list of MARC records
   */
  public void setMarcRecords(List<String> marcRecords) {
    this.marcRecords = marcRecords;
  }

  /**
   * Adds an ID to the list of failed IDs.
   *
   * @param id the UUID to add
   */
  public void addIdToFailed(UUID id) {
    failedIds.add(id);
  }

  /**
   * Adds an ID to the list of not existing IDs.
   *
   * @param id the UUID to add
   */
  public void addIdToNotExist(UUID id) {
    notExistIds.add(id);
  }
}
