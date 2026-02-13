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

/** Entity representing an item. */
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "v_item")
public class ItemEntity {

  /** Unique identifier of the item. */
  @Id private UUID id;

  /** Item details stored as JSONB. */
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private String jsonb;

  /** Holdings record ID associated with the item. */
  private UUID holdingsRecordId;
}
