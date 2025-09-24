package org.folio.dataexp.domain.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.dataexp.domain.dto.ErrorLog;
import org.hibernate.annotations.Type;

/**
 * Entity representing an error log entry.
 */
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "error_logs")
public class ErrorLogEntity {

  /**
   * Unique identifier of the error log.
   */
  @Id
  private UUID id;

  /**
   * Error log details stored as JSONB.
   */
  @Type(JsonBinaryType.class)
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private ErrorLog errorLog;

  /**
   * Date when the error log was created.
   */
  private Date creationDate;

  /**
   * User who created the error log.
   */
  private String createdBy;

  /**
   * Job execution ID associated with the error log.
   */
  private UUID jobExecutionId;

  /**
   * Job profile ID associated with the error log.
   */
  @Column(name = "jobprofileid")
  private UUID jobProfileId;
}
