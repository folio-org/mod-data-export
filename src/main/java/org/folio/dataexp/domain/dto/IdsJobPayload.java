package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/** DTO for job payload containing query and entity type. */
@Data
@With
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdsJobPayload {

  /** CQL query string. */
  private String query;

  /** Type of entity for the job. */
  private EntityType entityType;

  /** Supported entity types. */
  public enum EntityType {
    /** Instance entity type. */
    INSTANCE,
    /** Holdings entity type. */
    HOLDINGS,
    /** Authority entity type. */
    AUTHORITY
  }
}
