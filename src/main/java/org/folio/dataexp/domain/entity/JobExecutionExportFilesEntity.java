package org.folio.dataexp.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/**
 * Entity representing export files for a job execution.
 */
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "job_execution_export_files")
public class JobExecutionExportFilesEntity {

  /**
   * Unique identifier of the export file entry.
   */
  @Id
  private UUID id;

  /**
   * Job execution ID.
   */
  private UUID jobExecutionId;

  /**
   * Location of the exported file.
   */
  private String fileLocation;

  /**
   * Start ID for the export range.
   */
  private UUID fromId;

  /**
   * End ID for the export range.
   */
  private UUID toId;

  /**
   * Status of the export file.
   */
  @Enumerated(EnumType.STRING)
  private JobExecutionExportFilesStatus status;
}
