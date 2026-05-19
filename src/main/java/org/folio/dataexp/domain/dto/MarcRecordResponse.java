package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * To hold single record in the list of {@link MarcRecordsResponse}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarcRecordResponse {

  private ParsedRecord parsedRecord;
  private ExternalIdsHolder externalIdsHolder;
}
