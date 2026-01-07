package org.folio.dataexp.service;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionExportedFilesInner;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.exception.job.profile.DefaultJobProfileException;
import org.folio.dataexp.exception.job.profile.LockedJobProfileException;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JobProfileService}.
 *
 * <p>This test class covers all methods and scenarios in the JobProfileService, including: -
 * Successful deletion of job profiles - Exception handling for default and locked profiles -
 * Exported file deletion and link disabling - Job execution updates after profile deletion
 */
@ExtendWith(MockitoExtension.class)
class JobProfileServiceTest {

  @Mock private JobProfileEntityRepository jobProfileEntityRepository;

  @Mock private FolioS3Client s3Client;

  @Mock private JobExecutionService jobExecutionService;

  @InjectMocks private JobProfileService jobProfileService;

  @Captor private ArgumentCaptor<List<JobExecution>> jobExecutionListCaptor;

  private UUID jobProfileId;
  private JobProfileEntity jobProfileEntity;
  private JobProfile jobProfile;

  @BeforeEach
  void setUp() {
    jobProfileId = UUID.randomUUID();
    jobProfile = new JobProfile();
    jobProfile.setId(jobProfileId);
    jobProfile.setName("Test Profile");
    jobProfile.setDefault(FALSE);

    jobProfileEntity = new JobProfileEntity();
    jobProfileEntity.setId(jobProfileId);
    jobProfileEntity.setJobProfile(jobProfile);
    jobProfileEntity.setLocked(false);
  }

  @Test
  void shouldDeleteJobProfileSuccessfully_whenProfileIsNotDefaultAndNotLocked() {
    // Given
    UUID jobExecutionId = UUID.randomUUID();
    JobExecution jobExecution = createJobExecution(jobExecutionId, "Test Profile", jobProfileId);
    List<JobExecution> jobExecutions = List.of(jobExecution);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(jobExecutionService.getAllByJobProfileId(jobProfileId)).thenReturn(jobExecutions);

    // When
    jobProfileService.deleteJobProfileById(jobProfileId);

    // Then
    verify(jobProfileEntityRepository).getReferenceById(jobProfileId);
    verify(jobExecutionService).getAllByJobProfileId(jobProfileId);
    verify(jobExecutionService).saveAll(any());
    verify(jobProfileEntityRepository).deleteById(jobProfileId);
  }

  @Test
  void shouldThrowDefaultJobProfileException_whenProfileIsDefault() {
    // Given
    jobProfile.setDefault(TRUE);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);

    // When & Then
    assertThatThrownBy(() -> jobProfileService.deleteJobProfileById(jobProfileId))
        .isInstanceOf(DefaultJobProfileException.class)
        .hasMessage("Deletion of default job profile is forbidden");

    verify(jobProfileEntityRepository).getReferenceById(jobProfileId);
    verify(jobExecutionService, never()).getAllByJobProfileId(any());
    verify(jobProfileEntityRepository, never()).deleteById(any());
  }

  @Test
  void shouldThrowLockedJobProfileException_whenProfileIsLocked() {
    // Given
    jobProfileEntity.setLocked(true);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);

    // When & Then
    assertThatThrownBy(() -> jobProfileService.deleteJobProfileById(jobProfileId))
        .isInstanceOf(LockedJobProfileException.class)
        .hasMessage(
            "This profile is locked. Please unlock the profile to proceed with editing/deletion.");

    verify(jobProfileEntityRepository).getReferenceById(jobProfileId);
    verify(jobExecutionService, never()).getAllByJobProfileId(any());
    verify(jobProfileEntityRepository, never()).deleteById(any());
  }

  @Test
  void shouldDeleteExportedFilesAndUpdateJobExecutions_whenJobExecutionsExist() {
    // Given
    UUID jobExecutionId1 = UUID.randomUUID();
    UUID jobExecutionId2 = UUID.randomUUID();
    String fileName1 = "export1.mrc";
    String fileName2 = "export2.mrc";
    String fileName3 = "export3.mrc";

    JobExecution jobExecution1 = createJobExecution(jobExecutionId1, "Profile Name", jobProfileId);
    jobExecution1.setExportedFiles(createExportedFiles(fileName1, fileName2));

    JobExecution jobExecution2 = createJobExecution(jobExecutionId2, "Profile Name", jobProfileId);
    jobExecution2.setExportedFiles(createExportedFiles(fileName3));

    List<JobExecution> jobExecutions = List.of(jobExecution1, jobExecution2);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(jobExecutionService.getAllByJobProfileId(jobProfileId)).thenReturn(jobExecutions);

    // When
    jobProfileService.deleteJobProfileById(jobProfileId);

    // Then
    verify(s3Client, times(3)).remove(any(String.class));
    verify(jobExecutionService).saveAll(jobExecutionListCaptor.capture());

    List<JobExecution> savedExecutions = jobExecutionListCaptor.getValue();
    assertThat(savedExecutions).hasSize(2);

    // Verify first job execution
    JobExecution savedExecution1 = savedExecutions.getFirst();
    assertThat(savedExecution1.getJobProfileId()).isNull();
    assertThat(savedExecution1.getJobProfileName()).isEqualTo("Profile Name (deleted)");
    assertThat(savedExecution1.getExportedFiles()).hasSize(2);
    savedExecution1.getExportedFiles().forEach(file -> assertThat(file.getFileId()).isNull());

    // Verify second job execution
    JobExecution savedExecution2 = savedExecutions.get(1);
    assertThat(savedExecution2.getJobProfileId()).isNull();
    assertThat(savedExecution2.getJobProfileName()).isEqualTo("Profile Name (deleted)");
    assertThat(savedExecution2.getExportedFiles()).hasSize(1);
    savedExecution2.getExportedFiles().forEach(file -> assertThat(file.getFileId()).isNull());
  }

  @Test
  void shouldHandleJobExecutionsWithNoExportedFiles() {
    // Given
    UUID jobExecutionId = UUID.randomUUID();
    JobExecution jobExecution = createJobExecution(jobExecutionId, "Test Profile", jobProfileId);
    jobExecution.setExportedFiles(Collections.emptySet());

    List<JobExecution> jobExecutions = List.of(jobExecution);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(jobExecutionService.getAllByJobProfileId(jobProfileId)).thenReturn(jobExecutions);

    // When
    jobProfileService.deleteJobProfileById(jobProfileId);

    // Then
    verify(s3Client, never()).remove(any(String.class));
    verify(jobExecutionService).saveAll(jobExecutionListCaptor.capture());

    List<JobExecution> savedExecutions = jobExecutionListCaptor.getValue();
    assertThat(savedExecutions).hasSize(1);
    assertThat(savedExecutions.getFirst().getJobProfileId()).isNull();
    assertThat(savedExecutions.getFirst().getJobProfileName()).isEqualTo("Test Profile (deleted)");
  }

  @Test
  void shouldHandleEmptyJobExecutionsList() {
    // Given
    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(jobExecutionService.getAllByJobProfileId(jobProfileId))
        .thenReturn(Collections.emptyList());

    // When
    jobProfileService.deleteJobProfileById(jobProfileId);

    // Then
    verify(s3Client, never()).remove(any(String.class));
    verify(jobExecutionService).saveAll(Collections.emptyList());
    verify(jobProfileEntityRepository).deleteById(jobProfileId);
  }

  @Test
  void shouldDeleteJobProfileWithNullJobProfileName() {
    // Given
    UUID jobExecutionId = UUID.randomUUID();
    JobExecution jobExecution = createJobExecution(jobExecutionId, null, jobProfileId);
    jobExecution.setExportedFiles(Collections.emptySet());

    List<JobExecution> jobExecutions = List.of(jobExecution);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(jobExecutionService.getAllByJobProfileId(jobProfileId)).thenReturn(jobExecutions);

    // When
    jobProfileService.deleteJobProfileById(jobProfileId);

    // Then
    verify(jobExecutionService).saveAll(jobExecutionListCaptor.capture());
    List<JobExecution> savedExecutions = jobExecutionListCaptor.getValue();
    assertThat(savedExecutions.getFirst().getJobProfileName()).isEqualTo("null (deleted)");
  }

  @Test
  void shouldVerifyCorrectS3PathsAreUsed_whenDeletingFiles() {
    // Given
    UUID jobExecutionId = UUID.randomUUID();
    String fileName = "test-export.mrc";

    JobExecution jobExecution = createJobExecution(jobExecutionId, "Test Profile", jobProfileId);
    jobExecution.setExportedFiles(createExportedFiles(fileName));

    List<JobExecution> jobExecutions = List.of(jobExecution);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(jobExecutionService.getAllByJobProfileId(jobProfileId)).thenReturn(jobExecutions);

    // When
    jobProfileService.deleteJobProfileById(jobProfileId);

    // Then
    ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
    verify(s3Client).remove(pathCaptor.capture());

    String capturedPath = pathCaptor.getValue();
    assertThat(capturedPath).contains(jobExecutionId.toString());
    assertThat(capturedPath).contains(fileName);
  }

  @Test
  void shouldDeleteJobProfile_whenDefaultIsFalseExplicitly() {
    // Given
    jobProfile.setDefault(FALSE);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(jobExecutionService.getAllByJobProfileId(jobProfileId))
        .thenReturn(Collections.emptyList());

    // When
    jobProfileService.deleteJobProfileById(jobProfileId);

    // Then
    verify(jobProfileEntityRepository).deleteById(jobProfileId);
  }

  @Test
  void shouldDeleteJobProfile_whenDefaultIsNull() {
    // Given
    jobProfile.setDefault(null);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(jobExecutionService.getAllByJobProfileId(jobProfileId))
        .thenReturn(Collections.emptyList());

    // When
    jobProfileService.deleteJobProfileById(jobProfileId);

    // Then
    verify(jobProfileEntityRepository).deleteById(jobProfileId);
  }

  @Test
  void shouldHandleMultipleExportedFilesPerJobExecution() {
    // Given
    UUID jobExecutionId = UUID.randomUUID();
    JobExecution jobExecution = createJobExecution(jobExecutionId, "Test Profile", jobProfileId);

    Set<JobExecutionExportedFilesInner> exportedFiles = new HashSet<>();
    for (int i = 1; i <= 5; i++) {
      JobExecutionExportedFilesInner file =
          new JobExecutionExportedFilesInner()
              .fileId(UUID.randomUUID())
              .fileName("file" + i + ".mrc");
      exportedFiles.add(file);
    }
    jobExecution.setExportedFiles(exportedFiles);

    List<JobExecution> jobExecutions = List.of(jobExecution);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(jobExecutionService.getAllByJobProfileId(jobProfileId)).thenReturn(jobExecutions);

    // When
    jobProfileService.deleteJobProfileById(jobProfileId);

    // Then
    verify(s3Client, times(5)).remove(any(String.class));
    verify(jobExecutionService).saveAll(jobExecutionListCaptor.capture());

    List<JobExecution> savedExecutions = jobExecutionListCaptor.getValue();
    assertThat(savedExecutions.getFirst().getExportedFiles())
        .hasSize(5)
        .allMatch(file -> file.getFileId() == null);
  }

  // Helper methods

  /**
   * Creates a JobExecution instance with the given parameters.
   *
   * @param id the job execution ID
   * @param profileName the job profile name
   * @param profileId the job profile ID
   * @return a JobExecution instance
   */
  private JobExecution createJobExecution(UUID id, String profileName, UUID profileId) {
    JobExecution jobExecution = new JobExecution();
    jobExecution.setId(id);
    jobExecution.setJobProfileName(profileName);
    jobExecution.setJobProfileId(profileId);
    jobExecution.setExportedFiles(new HashSet<>());
    return jobExecution;
  }

  /**
   * Creates a set of JobExecutionExportedFilesInner instances with the given file names.
   *
   * @param fileNames the file names
   * @return a set of JobExecutionExportedFilesInner instances
   */
  private Set<JobExecutionExportedFilesInner> createExportedFiles(String... fileNames) {
    Set<JobExecutionExportedFilesInner> files = new HashSet<>();
    for (String fileName : fileNames) {
      JobExecutionExportedFilesInner file =
          new JobExecutionExportedFilesInner().fileId(UUID.randomUUID()).fileName(fileName);
      files.add(file);
    }
    return files;
  }
}
