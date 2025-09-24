package org.folio.dataexp.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;



/**
 * Entity representing a MARC record.
 */
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "v_marc_records_lb")
public class MarcRecordEntity {

  /**
   * Unique identifier of the MARC record.
   */
  @Id
  private UUID id;

  /**
   * External identifier associated with the MARC record.
   */
  private UUID externalId;

  /**
   * MARC record content stored as JSONB.
   */
  @Column(name = "content", columnDefinition = "jsonb")
  private String content;

  /**
   * Type of the MARC record.
   */
  private String recordType;

  /**
   * State of the MARC record.
   */
  private String state;

  /**
   * Leader record status character.
   */
  private Character leaderRecordStatus;

  /**
   * Indicates if the record is suppressed from discovery.
   */
  private Boolean suppressDiscovery;

  /**
   * Generation number of the MARC record.
   */
  private Integer generation;

  /**
   * Indicates if the record is deleted.
   */
  @Transient
  private boolean deleted;
}
