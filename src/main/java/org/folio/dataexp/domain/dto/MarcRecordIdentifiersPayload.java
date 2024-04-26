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
  private Boolean suppressFromDiscovery;
  private Boolean deleted;
  private Integer offset;
  private Integer limit;

}
