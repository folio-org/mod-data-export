package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** Represents holdings for a tenant. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Holdings {

  private String tenantId;
}
