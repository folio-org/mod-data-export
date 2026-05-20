package org.folio.dataexp.domain.dto.srsresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Child object of {@link MarcRecordResponse} to hold {@link Content}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedRecord {

  private Content content;
}
