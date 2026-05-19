package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Child object of {@link MarcRecordResponse} to hold {@link MarcContent}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedRecord {

  private MarcContent content;
}
