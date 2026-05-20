package org.folio.dataexp.domain.dto.srsresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

/**
 * To hold response from source storage when retrieving MARC
 * records by external IDs.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarcRecordsResponse {

  private List<MarcRecordResponse> sourceRecords;
  private int totalRecords;
}
