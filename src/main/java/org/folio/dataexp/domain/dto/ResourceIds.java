package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/**
 * DTO for resource IDs and total records.
 */
@Data
@With
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceIds {

  /**
   * List of resource IDs.
   */
  private List<Id> ids;

  /**
   * Total number of records.
   */
  private Integer totalRecords;

  /**
   * DTO for a single resource ID.
   */
  @Data
  @With
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Id {
    /**
     * Resource UUID.
     */
    private UUID id;
  }
}
