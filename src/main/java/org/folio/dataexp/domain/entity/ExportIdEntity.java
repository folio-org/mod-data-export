package org.folio.dataexp.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
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
@Table(name = "job_executions_export_ids")
public class ExportIdEntity {

  @Id
  @GeneratedValue
  private UUID id;

  private UUID jobExecutionId;

  private UUID instanceId;
}
