package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Represents a collection of holdings.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HoldingsCollection {

  @JsonProperty("consortia")
  private List<Holdings> holdings = new ArrayList<>();
}
