package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorityCollection {

  @JsonProperty("authorities")
  private List<Authority> authorities = new ArrayList<>();
}
