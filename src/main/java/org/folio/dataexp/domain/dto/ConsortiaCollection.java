package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Represents a collection of consortia entities.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsortiaCollection {

  @JsonProperty("consortia")
  private List<Consortia> consortia = new ArrayList<>();
}
