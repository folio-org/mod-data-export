package org.folio.dataexp.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

/** Entity representing export IDs for job executions. */
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "job_executions_export_ids")
public class ExportIdEntity {

  /** Unique identifier for the export ID entry. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  /** Job execution ID. */
  private UUID jobExecutionId;

  /** Instance ID. */
  private UUID instanceId;
}
