package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
  private String id;
  private String username;
  private Personal personal;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Personal {
    private String lastName;
    private String firstName;
  }
}

