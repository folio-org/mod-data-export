package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/** DTO for MARC records identifiers response. */
@Data
@With
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarcRecordsIdentifiersResponse {

  /** List of record identifiers. */
  private List<String> records;

  /** Total count of records. */
  private int totalCount;
}
