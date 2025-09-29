package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * DTO representing a collection of user tenants.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserTenantCollection {

  /**
   * List of user tenants.
   */
  @JsonProperty("userTenants")
  private List<UserTenant> userTenants = new ArrayList<>();
}
