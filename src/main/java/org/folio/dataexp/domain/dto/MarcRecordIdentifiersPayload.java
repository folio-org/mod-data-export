package org.folio.dataexp.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/** DTO for MARC record identifiers search payload. */
@Data
@With
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarcRecordIdentifiersPayload {

  /** Search expression for the leader field. */
  private String leaderSearchExpression;

  /** Search expression for MARC fields. */
  private String fieldsSearchExpression;

  /** Indicates if the record is suppressed from discovery. */
  private Boolean suppressFromDiscovery;

  /** Indicates if the record is deleted. */
  private Boolean deleted;

  /** Offset for pagination. */
  private Integer offset;

  /** Limit for pagination. */
  private Integer limit;
}
