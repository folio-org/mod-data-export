package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents a consortia entity.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Consortia {

  private String id;
}
