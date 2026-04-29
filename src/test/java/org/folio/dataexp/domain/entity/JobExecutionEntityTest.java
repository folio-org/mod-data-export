package org.folio.dataexp.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.UUID;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.JobExecution;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import org.folio.dataexp.domain.entity.JobExecutionEntity;

class JobExecutionEntityTest {

  @Test
  @TestMate(name = "TestMate-7709c7c3dd6a32b473f9e2ee5017ae88")
  void fromJobExecutionShouldGenerateIdWhenJobExecutionIdIsNull() {
    // Given
    var expectedGeneratedId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    var expectedDate = new Date(1672531200000L); // 2023-01-01T00:00:00Z
    var jobExecution = new JobExecution();
    jobExecution.setId(null);
    jobExecution.setJobProfileName("Test Profile");
    try (MockedStatic<UUID> mockedUuid = Mockito.mockStatic(UUID.class);
        MockedConstruction<Date> mockedDate =
            Mockito.mockConstruction(
                Date.class,
                (mock, context) -> {
                  if (context.arguments().isEmpty()) {
                    Mockito.when(mock.getTime()).thenReturn(expectedDate.getTime());
                  }
                })) {
      mockedUuid.when(UUID::randomUUID).thenReturn(expectedGeneratedId);
      // When
      var actualEntity = JobExecutionEntity.fromJobExecution(jobExecution);
      // Then
      assertThat(actualEntity.getId()).isEqualTo(expectedGeneratedId);
      assertThat(jobExecution.getId()).isEqualTo(expectedGeneratedId);
      assertThat(jobExecution.getLastUpdatedDate().getTime()).isEqualTo(expectedDate.getTime());
      assertThat(actualEntity.getJobExecution()).isSameAs(jobExecution);
      assertThat(actualEntity.getJobProfileName()).isEqualTo("Test Profile");
    }
  }

    @Test
  @TestMate(name = "TestMate-fromJobExecutionShouldHandleNullProgressAndRunBy")
  void fromJobExecutionShouldHandleNullProgressAndRunBy() {
    // TestMate-fc8e48f3ce15d9afef208241b05a524d
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var expectedTimestamp = 1705314600000L; // 2024-01-15T10:30:00Z
    var jobExecution = new JobExecution()
        .id(jobExecutionId)
        .status(JobExecution.StatusEnum.IN_PROGRESS)
        .progress(null)
        .runBy(null);
    try (MockedConstruction<Date> mockedDate = mockConstruction(Date.class, (mock, context) -> {
      if (context.arguments().isEmpty()) {
        when(mock.getTime()).thenReturn(expectedTimestamp);
      }
    })) {
      // When
      var actualEntity = JobExecutionEntity.fromJobExecution(jobExecution);
      // Then
      assertThat(actualEntity.getId()).isEqualTo(jobExecutionId);
      assertThat(actualEntity.getStatus()).isEqualTo(JobExecution.StatusEnum.IN_PROGRESS);
      
      assertThat(actualEntity.getTotal()).isNull();
      assertThat(actualEntity.getExported()).isNull();
      assertThat(actualEntity.getFailed()).isNull();
      
      assertThat(actualEntity.getRunById()).isNull();
      assertThat(actualEntity.getRunByFirstName()).isNull();
      assertThat(actualEntity.getRunByLastName()).isNull();
      assertThat(jobExecution.getLastUpdatedDate().getTime()).isEqualTo(expectedTimestamp);
      assertThat(actualEntity.getJobExecution()).isSameAs(jobExecution);
    }
  }

    @Test
  void fromJobExecutionShouldHandleNullDates() {
    // TestMate-5f4b4d93a95e17e1219e238028bf52ac
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var expectedTimestamp = 1705314600000L;
    var jobExecution = new JobExecution()
        .id(jobExecutionId)
        .status(JobExecution.StatusEnum.IN_PROGRESS)
        .startedDate(null)
        .completedDate(null);
    try (MockedConstruction<Date> mockedDate = mockConstruction(Date.class, (mock, context) -> {
      if (context.arguments().isEmpty()) {
        when(mock.getTime()).thenReturn(expectedTimestamp);
      }
    })) {
      // When
      var actualEntity = JobExecutionEntity.fromJobExecution(jobExecution);
      // Then
      assertThat(actualEntity.getStartedDate()).isNull();
      assertThat(actualEntity.getCompletedDate()).isNull();
      assertThat(actualEntity.getId()).isEqualTo(jobExecutionId);
      assertThat(actualEntity.getJobExecution()).isSameAs(jobExecution);
      assertThat(jobExecution.getLastUpdatedDate().getTime()).isEqualTo(expectedTimestamp);
    }
  }
}
