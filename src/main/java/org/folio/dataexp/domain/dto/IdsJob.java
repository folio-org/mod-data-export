package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/** Represents a job for processing IDs. */
@Data
@With
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdsJob {
  private UUID id;
  private Status status;

  /** Status of the job. */
  public enum Status {
    IN_PROGRESS,
    ERROR,
    COMPLETED,
    DEPRECATED
  }
}
