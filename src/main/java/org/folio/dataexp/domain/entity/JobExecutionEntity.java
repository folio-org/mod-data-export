package org.folio.dataexp.domain.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.dataexp.domain.dto.JobExecution;
import org.hibernate.annotations.Type;

import java.util.UUID;

@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "job_executions")
public class JobExecutionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Type(JsonBinaryType.class)
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private JobExecution jobExecution;

  private UUID jobProfileId;
}
