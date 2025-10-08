package org.folio.dataexp.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for FQM contents results for Linked Data entity types.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LinkedDataResource {
  /**
   * Inventory UUID.
   */
  private String inventoryId;

  /**
   * Resource subgraph internal Linked Data JSON.
   */
  private String resource;
}
