package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents an authority record.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Authority {
  private String id;
}
