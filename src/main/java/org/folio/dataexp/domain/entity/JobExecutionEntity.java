package org.folio.dataexp.domain.entity;

import static java.util.Objects.isNull;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.folio.dataexp.domain.dto.JobExecution;
import org.hibernate.annotations.Type;

/** Entity representing a job execution. */
@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "job_executions")
public class JobExecutionEntity {

  /** Unique identifier of the job execution. */
  @Id private UUID id;

  /** Job execution details stored as JSONB. */
  @Type(JsonBinaryType.class)
  @Column(name = "jsonb", columnDefinition = "jsonb")
  private JobExecution jobExecution;

  /** Human-readable ID. */
  private Integer hrid;

  /** Total records to export. */
  private Integer total;

  /** Number of exported records. */
  private Integer exported;

  /** Number of failed records. */
  private Integer failed;

  /** Job profile ID. */
  @Column(name = "jobprofileid")
  private UUID jobProfileId;

  /** Job profile name. */
  private String jobProfileName;

  /** Date when the job started. */
  private LocalDateTime startedDate;

  /** Date when the job completed. */
  private LocalDateTime completedDate;

  /** User ID who ran the job. */
  private UUID runById;

  /** First name of the user who ran the job. */
  private String runByFirstName;

  /** Last name of the user who ran the job. */
  private String runByLastName;

  /** Status of the job execution. */
  @Enumerated(EnumType.STRING)
  private JobExecution.StatusEnum status;

  /** Creates a JobExecutionEntity from a JobExecution DTO. */
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
        .exported(
            isNull(jobExecution.getProgress()) ? null : jobExecution.getProgress().getExported())
        .failed(isNull(jobExecution.getProgress()) ? null : jobExecution.getProgress().getFailed())
        .jobProfileId(jobExecution.getJobProfileId())
        .jobProfileName(jobExecution.getJobProfileName())
        .startedDate(
            isNull(jobExecution.getStartedDate())
                ? null
                : jobExecution
                    .getStartedDate()
                    .toInstant()
                    .atZone(ZoneOffset.UTC)
                    .toLocalDateTime())
        .completedDate(
            isNull(jobExecution.getCompletedDate())
                ? null
                : jobExecution
                    .getCompletedDate()
                    .toInstant()
                    .atZone(ZoneOffset.UTC)
                    .toLocalDateTime())
        .runById(
            isNull(jobExecution.getRunBy())
                ? null
                : UUID.fromString(jobExecution.getRunBy().getUserId()))
        .runByFirstName(
            isNull(jobExecution.getRunBy()) ? null : jobExecution.getRunBy().getFirstName())
        .runByLastName(
            isNull(jobExecution.getRunBy()) ? null : jobExecution.getRunBy().getLastName())
        .status(jobExecution.getStatus())
        .build();
  }
}
