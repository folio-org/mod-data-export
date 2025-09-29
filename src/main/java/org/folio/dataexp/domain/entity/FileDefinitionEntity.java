package org.folio.dataexp.domain.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.hibernate.annotations.Type;


/**
 * Entity representing a file definition.
 */
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "file_definitions")
public class FileDefinitionEntity {

  /**
   * Unique identifier of the file definition.
   */
  @Id
  private UUID id;

  /**
   * File definition details stored as JSONB.
   */
  @Type(JsonBinaryType.class)
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private FileDefinition fileDefinition;

  /**
   * Date when the file definition was created.
   */
  private LocalDateTime creationDate;

  /**
   * User who created the file definition.
   */
  private String createdBy;
}
