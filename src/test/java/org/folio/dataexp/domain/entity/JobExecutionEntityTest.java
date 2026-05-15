package org.folio.dataexp.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.UUID;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.JobExecution;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.dto.JobExecutionRunBy;

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
  @TestMate(name = "TestMate-fc8e48f3ce15d9afef208241b05a524d")
  void fromJobExecutionShouldHandleNullProgressAndRunBy() {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var expectedTimestamp = 1705314600000L; // 2024-01-15T10:30:00Z
    var jobExecution =
        new JobExecution()
            .id(jobExecutionId)
            .status(JobExecution.StatusEnum.IN_PROGRESS)
            .progress(null)
            .runBy(null);
    try (MockedConstruction<Date> mockedDate =
        mockConstruction(
            Date.class,
            (mock, context) -> {
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
  @TestMate(name = "TestMate-5f4b4d93a95e17e1219e238028bf52ac")
  void fromJobExecutionShouldHandleNullDates() {
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var expectedTimestamp = 1705314600000L;
    var jobExecution =
        new JobExecution()
            .id(jobExecutionId)
            .status(JobExecution.StatusEnum.IN_PROGRESS)
            .startedDate(null)
            .completedDate(null);
    try (MockedConstruction<Date> mockedDate =
        mockConstruction(
            Date.class,
            (mock, context) -> {
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

    @Test
  @TestMate(name = "TestMate-f98234b1c2d3e4f5a6b7c8d9e0f1a2b3")
  void fromJobExecutionShouldMapAllFieldsWhenFullyPopulated() {
    // TestMate-a3e2ca6ca1e9d288b1b82c5ff0e5bc9a
    // Given
    var jobExecutionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var jobProfileId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var userId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    
    var startedTimestamp = 1704103200000L; // 2024-01-01T10:00:00Z
    var completedTimestamp = 1704106800000L; // 2024-01-01T11:00:00Z
    var currentTimestamp = 1704110400000L; // 2024-01-01T12:00:00Z
    var jobExecution = new JobExecution()
        .id(jobExecutionId)
        .hrId(1001)
        .status(JobExecution.StatusEnum.COMPLETED)
        .jobProfileId(jobProfileId)
        .jobProfileName("Test Profile")
        .progress(new JobExecutionProgress().total(50).exported(45).failed(5))
        .runBy(new JobExecutionRunBy().userId(userId.toString()).firstName("John").lastName("Doe"))
        .startedDate(new Date(startedTimestamp))
        .completedDate(new Date(completedTimestamp));
    try (MockedConstruction<Date> mockedDate = mockConstruction(Date.class, (mock, context) -> {
      if (context.arguments().isEmpty()) {
        when(mock.getTime()).thenReturn(currentTimestamp);
      }
    })) {
      // When
      var actualEntity = JobExecutionEntity.fromJobExecution(jobExecution);
      // Then
      assertThat(actualEntity.getId()).isEqualTo(jobExecutionId);
      assertThat(actualEntity.getJobExecution()).isSameAs(jobExecution);
      assertThat(actualEntity.getHrid()).isEqualTo(1001);
      assertThat(actualEntity.getStatus()).isEqualTo(JobExecution.StatusEnum.COMPLETED);
      assertThat(actualEntity.getTotal()).isEqualTo(50);
      assertThat(actualEntity.getExported()).isEqualTo(45);
      assertThat(actualEntity.getFailed()).isEqualTo(5);
      assertThat(actualEntity.getJobProfileId()).isEqualTo(jobProfileId);
      assertThat(actualEntity.getJobProfileName()).isEqualTo("Test Profile");
      assertThat(actualEntity.getRunById()).isEqualTo(userId);
      assertThat(actualEntity.getRunByFirstName()).isEqualTo("John");
      assertThat(actualEntity.getRunByLastName()).isEqualTo("Doe");
      assertThat(actualEntity.getStartedDate()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
      assertThat(actualEntity.getCompletedDate()).isEqualTo(LocalDateTime.of(2024, 1, 1, 11, 0, 0));
      
      assertThat(jobExecution.getLastUpdatedDate().getTime()).isEqualTo(currentTimestamp);
    }
  }

    @Test
  void fromJobExecutionShouldPreserveExistingId() {
    // TestMate-fdab45d20dedc42cc7e5d752be97110a
    // Given
    var existingId = UUID.fromString("00000000-0000-0000-0000-00000000000A");
    var expectedTimestamp = 1705314600000L;
    var jobExecution = new JobExecution().id(existingId);
    try (MockedStatic<UUID> mockedUuid = Mockito.mockStatic(UUID.class);
        MockedConstruction<Date> mockedDate = mockConstruction(Date.class, (mock, context) -> {
          if (context.arguments().isEmpty()) {
            when(mock.getTime()).thenReturn(expectedTimestamp);
          }
        })) {
      // When
      var actualEntity = JobExecutionEntity.fromJobExecution(jobExecution);
      // Then
      assertThat(actualEntity.getId()).isEqualTo(existingId);
      assertThat(jobExecution.getId()).isEqualTo(existingId);
      assertThat(jobExecution.getLastUpdatedDate().getTime()).isEqualTo(expectedTimestamp);
      assertThat(actualEntity.getJobExecution()).isSameAs(jobExecution);
      mockedUuid.verify(UUID::randomUUID, Mockito.never());
    }
  }
}
