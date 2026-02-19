package org.folio.dataexp.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.repository.JobExecutionEntityCqlRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.any;
import org.folio.dataexp.service.JobExecutionService;
import org.folio.dataexp.service.JobProfileService;
import static org.mockito.Mockito.never;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class JobExecutionServiceTest {

    @Mock
private JobExecutionEntityCqlRepository jobExecutionEntityCqlRepository;

    @InjectMocks
private JobExecutionService jobExecutionService;

    @Mock
    private JobProfileService jobProfileService;

    @Test
void setCompletedDateForFailedExecutionsIfRequiredShouldSetLastUpdatedDateWhenAvailable() {
    // TestMate-3db225a034406710ed2ebf6ca229f351
    // Given
    var jobExecutionId = UUID.fromString("111e4567-e89b-12d3-a456-426614174000");
    var lastUpdatedDate = new Date(1698397200000L); // 2023-10-27T10:00:00Z
    var jobExecution = new JobExecution();
    jobExecution.setId(jobExecutionId);
    jobExecution.setStatus(JobExecution.StatusEnum.FAIL);
    jobExecution.setLastUpdatedDate(lastUpdatedDate);
    jobExecution.setCompletedDate(null);
    var jobExecutionEntity = JobExecutionEntity.builder()
      .id(jobExecutionId)
      .jobExecution(jobExecution)
      .build();
    when(jobExecutionEntityCqlRepository.getFailedExecutionsWithoutCompletedDate()).thenReturn(List.of(jobExecutionEntity));
    when(jobExecutionEntityCqlRepository.save(org.mockito.ArgumentMatchers.any(JobExecutionEntity.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    var captor = ArgumentCaptor.forClass(JobExecutionEntity.class);
    // When
    jobExecutionService.setCompletedDateForFailedExecutionsIfRequired();
    // Then
    verify(jobExecutionEntityCqlRepository).save(captor.capture());
    var capturedEntity = captor.getValue();
    var capturedJobExecution = capturedEntity.getJobExecution();
    assertThat(capturedJobExecution.getCompletedDate()).isEqualTo(lastUpdatedDate);
    assertThat(capturedJobExecution.getId()).isEqualTo(jobExecutionId);
}

    @Test
    void setCompletedDateForFailedExecutionsIfRequiredShouldSetCurrentDateWhenLastUpdatedDateIsNull() {
        // TestMate-cdc5edee454f195c44d3184518f38806
        // Given
        var jobExecutionId = UUID.fromString("222e4567-e89b-12d3-a456-426614174000");
        var expectedDate = new Date(1672531200000L); // 2023-01-01T00:00:00Z
        var jobExecution = new JobExecution();
        jobExecution.setId(jobExecutionId);
        jobExecution.setStatus(JobExecution.StatusEnum.FAIL);
        jobExecution.setLastUpdatedDate(null);
        jobExecution.setCompletedDate(null);
        var jobExecutionEntity = JobExecutionEntity.builder()
                .id(jobExecutionId)
                .jobExecution(jobExecution)
                .build();
        when(jobExecutionEntityCqlRepository.getFailedExecutionsWithoutCompletedDate()).thenReturn(List.of(jobExecutionEntity));
        when(jobExecutionEntityCqlRepository.save(any(JobExecutionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var captor = ArgumentCaptor.forClass(JobExecutionEntity.class);
        try (MockedConstruction<Date> mockedDate = Mockito.mockConstruction(Date.class, (mock, context) -> {
            if (context.arguments().isEmpty()) {
                when(mock.getTime()).thenReturn(expectedDate.getTime());
                when(mock.toInstant()).thenReturn(expectedDate.toInstant());
            }
        })) {
            // When
            jobExecutionService.setCompletedDateForFailedExecutionsIfRequired();
            // Then
            verify(jobExecutionEntityCqlRepository).save(captor.capture());
            var capturedEntity = captor.getValue();
            var capturedJobExecution = capturedEntity.getJobExecution();
            assertThat(capturedJobExecution.getCompletedDate().getTime()).isEqualTo(expectedDate.getTime());
            assertThat(capturedJobExecution.getId()).isEqualTo(jobExecutionId);
        }
    }

    @Test
void setCompletedDateForFailedExecutionsIfRequiredShouldDoNothingWhenNoFailedExecutions() {
    // TestMate-eade8086f13d298ac8279b8d6039a12b
    // Given
    when(jobExecutionEntityCqlRepository.getFailedExecutionsWithoutCompletedDate()).thenReturn(Collections.emptyList());
    // When
    jobExecutionService.setCompletedDateForFailedExecutionsIfRequired();
    // Then
    verify(jobExecutionEntityCqlRepository, never()).save(any(JobExecutionEntity.class));
}
}
