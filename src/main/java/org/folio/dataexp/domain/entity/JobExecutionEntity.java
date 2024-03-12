package org.folio.dataexp.domain.entity;

import static java.util.Objects.isNull;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
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
import org.folio.dataexp.domain.dto.JobExecution;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "job_executions")
public class JobExecutionEntity {

  @Id
  private UUID id;

  @Type(JsonBinaryType.class)
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private JobExecution jobExecution;

  private Integer hrid;
  private Integer total;
  private Integer exported;
  private Integer failed;

  @Column(name = "jobprofileid")
  private UUID jobProfileId;

  private String jobProfileName;
  private LocalDateTime startedDate;
  private LocalDateTime completedDate;
  private UUID runById;
  private String runByFirstName;
  private String runByLastName;

  @Enumerated(EnumType.STRING)
  private JobExecution.StatusEnum status;

  public static JobExecutionEntity fromJobExecution(JobExecution jobExecution) {
    if (isNull(jobExecution.getId())) {
      jobExecution.setId(UUID.randomUUID());
    }
    jobExecution.setLastUpdatedDate(new Date());
    return JobExecutionEntity.builder()
      .id(jobExecution.getId())
      .jobExecution(jobExecution)
      .hrid(jobExecution.getHrId())
      .total(isNull(jobExecution.getProgress()) ? null : jobExecution.getProgress().getTotal())
      .exported(isNull(jobExecution.getProgress()) ? null : jobExecution.getProgress().getExported())
      .failed(isNull(jobExecution.getProgress()) ? null : jobExecution.getProgress().getFailed())
      .jobProfileId(jobExecution.getJobProfileId())
      .jobProfileName(jobExecution.getJobProfileName())
      .startedDate(isNull(jobExecution.getStartedDate()) ? null : jobExecution.getStartedDate().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime())
      .completedDate(isNull(jobExecution.getCompletedDate()) ? null : jobExecution.getCompletedDate().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime())
      .runById(isNull(jobExecution.getRunBy()) ? null : UUID.fromString(jobExecution.getRunBy().getUserId()))
      .runByFirstName(isNull(jobExecution.getRunBy()) ? null : jobExecution.getRunBy().getFirstName())
      .runByLastName(isNull(jobExecution.getRunBy()) ? null : jobExecution.getRunBy().getLastName())
      .status(jobExecution.getStatus())
      .build();
  }
}
