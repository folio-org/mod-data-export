package org.folio.dataexp.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.UUID;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "job_execution_export_files")
public class JobExecutionExportFilesEntity {

  @Id
  private UUID id;

  private UUID jobExecutionId;

  private String fileLocation;

  private UUID fromId;

  private UUID toId;

  private String status;
}
