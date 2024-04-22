package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarcRecordIdentifiersPayload {

  private String leaderSearchExpression;
  private String fieldsSearchExpression;
  private boolean suppressFromDiscovery;
  private boolean deleted;
  private int offset;
  private int limit;

}
