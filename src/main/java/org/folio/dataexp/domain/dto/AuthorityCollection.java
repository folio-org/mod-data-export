package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** Represents a collection of authority records. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorityCollection {

  @JsonProperty("authorities")
  private List<Authority> authorities = new ArrayList<>();
}
