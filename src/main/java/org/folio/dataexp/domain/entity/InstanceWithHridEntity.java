package org.folio.dataexp.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/** Entity representing an instance with HRID. */
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "v_instance_hrid")
public class InstanceWithHridEntity {

  /** Unique identifier of the instance. */
  @Id private UUID id;

  /** Human-readable ID of the instance. */
  private String hrid;
}
