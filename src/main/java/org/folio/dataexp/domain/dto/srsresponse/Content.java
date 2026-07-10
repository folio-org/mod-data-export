package org.folio.dataexp.domain.dto.srsresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Child object of {@link ParsedRecord} that contains the leader.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Content {

  private String leader;
}
