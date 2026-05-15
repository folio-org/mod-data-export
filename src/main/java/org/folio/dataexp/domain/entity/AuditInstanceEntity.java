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

/** Entity representing an audit instance. */
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "v_audit_instance")
public class AuditInstanceEntity {

  /** Unique identifier of the audit instance. */
  @Id private UUID id;

  /** Title of the audit instance. */
  private String title;

  /** Human-readable ID of the audit instance. */
  private String hrid;
}
