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
}
