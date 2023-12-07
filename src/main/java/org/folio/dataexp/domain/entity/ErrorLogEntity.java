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
import org.folio.dataexp.domain.dto.ErrorLog;
import org.hibernate.annotations.Type;

import java.util.Date;
import java.util.UUID;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "error_logs")
public class ErrorLogEntity {

  @Id
  private UUID id;

  @Type(JsonBinaryType.class)
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private ErrorLog errorLog;

  private Date creationDate;

  private String createdBy;

  private UUID jobExecutionId;

  private UUID jobProfileId;
}
