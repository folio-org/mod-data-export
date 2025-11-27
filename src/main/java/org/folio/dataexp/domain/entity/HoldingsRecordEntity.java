package org.folio.dataexp.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/** Entity representing a holdings record. */
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "v_holdings_record")
public class HoldingsRecordEntity {

  /** Unique identifier of the holdings record. */
  @Id private UUID id;

  /** Holdings record details stored as JSONB. */
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private String jsonb;

  /** Instance ID associated with the holdings record. */
  private UUID instanceId;
}
