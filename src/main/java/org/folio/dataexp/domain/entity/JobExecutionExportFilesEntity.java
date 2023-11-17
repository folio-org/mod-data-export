package org.folio.dataexp.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.dataexp.service.export.strategies.ExportStrategyStatistic;

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

  @Enumerated(EnumType.STRING)
  private JobExecutionExportFilesStatus status;

  public void setStatusBaseExportStatistic(ExportStrategyStatistic exportStatistic) {
    if (exportStatistic.getFailed() == 0 && exportStatistic.getExported() > 0) this.status = JobExecutionExportFilesStatus.COMPLETED;
    if (exportStatistic.getFailed() > 0 && exportStatistic.getExported() > 0) this.status = JobExecutionExportFilesStatus.COMPLETED_WITH_ERRORS;
    if (exportStatistic.getFailed() >= 0 && exportStatistic.getExported() == 0) this.status = JobExecutionExportFilesStatus.FAILED;
  }
}
