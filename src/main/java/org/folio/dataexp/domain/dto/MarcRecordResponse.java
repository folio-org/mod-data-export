package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarcRecordResponse {

  private ParsedRecord parsedRecord;
  private ExternalIdsHolder externalIdsHolder;
}
