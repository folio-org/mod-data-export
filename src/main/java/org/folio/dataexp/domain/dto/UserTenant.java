package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** DTO representing a user tenant. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserTenant {
  /** Central tenant ID. */
  private String centralTenantId;

  /** Tenant ID. */
  private String tenantId;
}
