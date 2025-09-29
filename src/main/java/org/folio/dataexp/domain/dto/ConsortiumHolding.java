package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents a consortium holding record.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsortiumHolding {
  private String id;
  private String tenantId;
  private String instanceId;
}
