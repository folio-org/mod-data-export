package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/**
 * DTO representing user permissions.
 */
@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissions {

  /**
   * List of permission names.
   */
  @JsonProperty("permissionNames")
  @Builder.Default
  private List<String> permissionNames = new ArrayList<>();

  /**
   * List of permissions.
   */
  @JsonProperty("permissions")
  @Builder.Default
  private List<String> permissions = new ArrayList<>();
}
