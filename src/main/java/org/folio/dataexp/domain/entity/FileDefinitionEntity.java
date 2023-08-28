package org.folio.dataexp.domain.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "file_definitions")
public class FileDefinitionEntity {

  @Id
  private UUID id;

  @Type(JsonBinaryType.class)
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private FileDefinition fileDefinition;

  private LocalDateTime creationDate;

  private String createdBy;
}
