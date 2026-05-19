package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * To hold instanceId for {@link MarcRecordResponse}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalIdsHolder {

  private String instanceId;
}
