package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** DTO representing a user. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
  /** User ID. */
  private String id;

  /** Username. */
  private String username;

  /** Personal information. */
  private Personal personal;

  /** DTO for personal information. */
  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Personal {
    /** Last name. */
    private String lastName;

    /** First name. */
    private String firstName;
  }
}
