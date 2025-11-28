package org.folio.dataexp.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/** Entity representing a configuration key-value pair. */
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "config_data")
public class ConfigurationEntity {

  /** Configuration key. */
  @Id private String key;

  /** Configuration value. */
  private String value;
}
