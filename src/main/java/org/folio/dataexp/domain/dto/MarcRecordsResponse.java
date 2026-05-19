package org.folio.dataexp.domain.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarcRecordsResponse {

  private List<MarcRecordResponse> sourceRecords;
  private int totalRecords;
}
