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
 * Entity representing an instance.
 */
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "v_instance")
public class InstanceEntity {

  /**
   * Unique identifier of the instance.
   */
  @Id
  private UUID id;

  /**
   * Instance details stored as JSONB.
   */
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private String jsonb;

  /**
   * Indicates if the instance is deleted.
   */
  @Transient
  private boolean deleted;
}
