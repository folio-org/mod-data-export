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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionExportedFilesInner;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.JobProfileCollection;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.User;
import org.folio.dataexp.domain.dto.UserInfo;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.exception.job.profile.DefaultJobProfileException;
import org.folio.dataexp.exception.job.profile.LockJobProfileException;
import org.folio.dataexp.exception.job.profile.LockJobProfilePermissionException;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.repository.JobProfileEntityCqlRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.service.validators.PermissionsValidator;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class JobProfileServiceTest {

  @Mock private JobProfileEntityRepository jobProfileEntityRepository;

  @Mock private FolioS3Client s3Client;

  @Mock private JobExecutionService jobExecutionService;
  @Mock private ErrorLogEntityCqlRepository errorLogEntityCqlRepository;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private UserClient userClient;
  @Mock private PermissionsValidator permissionsValidator;

  @InjectMocks private JobProfileService jobProfileService;

  @Captor private ArgumentCaptor<List<JobExecution>> jobExecutionListCaptor;
  @Captor private ArgumentCaptor<JobProfileEntity> jobProfileEntityCaptor;

  @Mock private JobProfileEntityCqlRepository jobProfileEntityCqlRepository;

  private UUID jobProfileId;
  private JobProfileEntity jobProfileEntity;
  private JobProfile jobProfile;
  private UUID userId;
  private User user;

  @BeforeEach
  void setUp() {
    jobProfileId = UUID.randomUUID();
    userId = UUID.randomUUID();

    jobProfile = new JobProfile();
    jobProfile.setId(jobProfileId);
    jobProfile.setName("Test Profile");
    jobProfile.setDefault(FALSE);
    jobProfile.setLocked(FALSE);

    var metadata = new Metadata();
    metadata.setCreatedDate(new Date());
    metadata.setUpdatedDate(new Date());
    metadata.setCreatedByUserId(userId.toString());
    metadata.setUpdatedByUserId(userId.toString());
    metadata.setCreatedByUsername("testuser");
    metadata.setUpdatedByUsername("testuser");
    jobProfile.setMetadata(metadata);

    jobProfileEntity = new JobProfileEntity();
    jobProfileEntity.setId(jobProfileId);
    jobProfileEntity.setJobProfile(jobProfile);
    jobProfileEntity.setLocked(false);

    user = new User();
    user.setId(userId.toString());
    user.setUsername("testuser");
    var personal = new User.Personal();
    personal.setFirstName("Test");
    personal.setLastName("User");
    user.setPersonal(personal);
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
        .isInstanceOf(LockJobProfileException.class)
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
    assertThat(savedExecution1.getJobProfileName()).isEqualTo("Profile Name");
    assertThat(savedExecution1.getExportedFiles()).hasSize(2);
    savedExecution1.getExportedFiles().forEach(file -> assertThat(file.getFileId()).isNull());

    // Verify second job execution
    JobExecution savedExecution2 = savedExecutions.get(1);
    assertThat(savedExecution2.getJobProfileId()).isNull();
    assertThat(savedExecution2.getJobProfileName()).isEqualTo("Profile Name");
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
    assertThat(savedExecutions.getFirst().getJobProfileName()).isEqualTo("Test Profile");
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
    assertThat(savedExecutions.getFirst().getJobProfileName()).isNull();
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
    assertThat(capturedPath).contains(jobExecutionId.toString()).contains(fileName);
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

  @ParameterizedTest
  @CsvSource({"0", "5", "1000"})
  void shouldDeleteAssociatedErrors_withVariousErrorCounts(long deletedErrorCount) {
    // Given
    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(jobExecutionService.getAllByJobProfileId(jobProfileId))
        .thenReturn(Collections.emptyList());
    when(errorLogEntityCqlRepository.deleteByJobProfileId(jobProfileId))
        .thenReturn(deletedErrorCount);

    // When
    jobProfileService.deleteJobProfileById(jobProfileId);

    // Then
    verify(errorLogEntityCqlRepository).deleteByJobProfileId(jobProfileId);
    verify(jobProfileEntityRepository).deleteById(jobProfileId);
  }

  @Test
  void shouldDeleteAssociatedErrors_beforeDeletingJobProfile() {
    // Given
    long deletedErrorCount = 10L;
    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(jobExecutionService.getAllByJobProfileId(jobProfileId))
        .thenReturn(Collections.emptyList());
    when(errorLogEntityCqlRepository.deleteByJobProfileId(jobProfileId))
        .thenReturn(deletedErrorCount);

    // When
    jobProfileService.deleteJobProfileById(jobProfileId);

    // Then
    var inOrder =
        org.mockito.Mockito.inOrder(
            errorLogEntityCqlRepository, jobProfileEntityRepository, jobExecutionService);
    inOrder.verify(errorLogEntityCqlRepository).deleteByJobProfileId(jobProfileId);
    inOrder.verify(jobExecutionService).getAllByJobProfileId(jobProfileId);
    inOrder.verify(jobProfileEntityRepository).deleteById(jobProfileId);
  }

  @Test
  void shouldNotDeleteAssociatedErrors_whenJobProfileIsDefault() {
    // Given
    jobProfile.setDefault(TRUE);
    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);

    // When & Then
    assertThatThrownBy(() -> jobProfileService.deleteJobProfileById(jobProfileId))
        .isInstanceOf(DefaultJobProfileException.class);

    verify(errorLogEntityCqlRepository, never()).deleteByJobProfileId(any());
    verify(jobProfileEntityRepository, never()).deleteById(any());
  }

  @Test
  void shouldNotDeleteAssociatedErrors_whenJobProfileIsLocked() {
    // Given
    jobProfileEntity.setLocked(true);
    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);

    // When & Then
    assertThatThrownBy(() -> jobProfileService.deleteJobProfileById(jobProfileId))
        .isInstanceOf(LockJobProfileException.class);

    verify(errorLogEntityCqlRepository, never()).deleteByJobProfileId(any());
    verify(jobProfileEntityRepository, never()).deleteById(any());
  }

  @Test
  void shouldDeleteAssociatedErrorsWithJobExecutionsAndFiles() {
    // Given
    UUID jobExecutionId = UUID.randomUUID();
    String fileName = "export.mrc";

    JobExecution jobExecution = createJobExecution(jobExecutionId, "Test Profile", jobProfileId);
    jobExecution.setExportedFiles(createExportedFiles(fileName));
    List<JobExecution> jobExecutions = List.of(jobExecution);
    long deletedErrorCount = 3L;

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(jobExecutionService.getAllByJobProfileId(jobProfileId)).thenReturn(jobExecutions);
    when(errorLogEntityCqlRepository.deleteByJobProfileId(jobProfileId))
        .thenReturn(deletedErrorCount);

    // When
    jobProfileService.deleteJobProfileById(jobProfileId);

    // Then
    verify(errorLogEntityCqlRepository).deleteByJobProfileId(jobProfileId);
    verify(s3Client).remove(any(String.class));
    verify(jobExecutionService).saveAll(any());
    verify(jobProfileEntityRepository).deleteById(jobProfileId);
  }

  @Test
  void shouldReturnTrue_whenJobProfileExists() {
    // Given
    UUID existingJobProfileId = UUID.randomUUID();
    when(jobProfileEntityRepository.existsById(existingJobProfileId)).thenReturn(true);

    // When
    boolean result = jobProfileService.jobProfileExists(existingJobProfileId);

    // Then
    assertThat(result).isTrue();
    verify(jobProfileEntityRepository).existsById(existingJobProfileId);
  }

  @Test
  void shouldReturnFalse_whenJobProfileDoesNotExist() {
    // Given
    UUID nonExistingJobProfileId = UUID.randomUUID();
    when(jobProfileEntityRepository.existsById(nonExistingJobProfileId)).thenReturn(false);

    // When
    boolean result = jobProfileService.jobProfileExists(nonExistingJobProfileId);

    // Then
    assertThat(result).isFalse();
    verify(jobProfileEntityRepository).existsById(nonExistingJobProfileId);
  }

  @Test
  void shouldReturnFalse_whenJobProfileIdIsNull() {
    // When
    boolean result = jobProfileService.jobProfileExists(null);

    // Then
    assertThat(result).isFalse();
    verify(jobProfileEntityRepository, never()).existsById(any());
  }

  @Test
  void shouldNotInvokeRepository_whenJobProfileIdIsNull() {
    // When
    jobProfileService.jobProfileExists(null);

    // Then
    verify(jobProfileEntityRepository, never()).existsById(any());
  }

  // ========== Tests for putJobProfile with updateLock, lockProfile, and unlockProfile ==========

  @Test
  void shouldLockProfile_whenLockStatusChangesFromFalseToTrue() {
    // Given
    JobProfile updatedProfile = new JobProfile();
    updatedProfile.setId(jobProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(TRUE);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockJobProfilePermission()).thenReturn(true);
    when(jobProfileEntityRepository.save(any(JobProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    jobProfileService.putJobProfile(jobProfileId, updatedProfile);

    // Then
    verify(permissionsValidator).checkLockJobProfilePermission();
    verify(jobProfileEntityRepository).save(jobProfileEntityCaptor.capture());

    JobProfile savedProfile = jobProfileEntityCaptor.getValue().getJobProfile();
    assertThat(savedProfile.getLocked()).isTrue();
    assertThat(savedProfile.getLockedAt()).isNotNull();
    assertThat(savedProfile.getLockedBy()).isEqualTo(userId);
  }

  @Test
  void shouldUnlockProfile_whenLockStatusChangesFromTrueToFalse() {
    // Given
    jobProfileEntity.setLocked(true);
    jobProfile.setLocked(true);

    JobProfile updatedProfile = new JobProfile();
    updatedProfile.setId(jobProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(FALSE);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockJobProfilePermission()).thenReturn(true);
    when(jobProfileEntityRepository.save(any(JobProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    jobProfileService.putJobProfile(jobProfileId, updatedProfile);

    // Then
    verify(permissionsValidator).checkLockJobProfilePermission();
    verify(jobProfileEntityRepository).save(jobProfileEntityCaptor.capture());

    JobProfile savedProfile = jobProfileEntityCaptor.getValue().getJobProfile();
    assertThat(savedProfile.getLocked()).isFalse();
    assertThat(savedProfile.getLockedAt()).isNull();
    assertThat(savedProfile.getLockedBy()).isNull();
  }

  @Test
  void shouldNotChangeLock_whenLockStatusRemainsUnchanged() {
    // Given
    JobProfile updatedProfile = new JobProfile();
    updatedProfile.setId(jobProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(FALSE);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(jobProfileEntityRepository.save(any(JobProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    jobProfileService.putJobProfile(jobProfileId, updatedProfile);

    // Then
    verify(permissionsValidator, never()).checkLockJobProfilePermission();
    verify(permissionsValidator, never()).checkLockJobProfilePermission();
  }

  @Test
  void shouldThrowLockJobProfilePermissionException_whenUserHasNoLockPermission() {
    // Given
    JobProfile updatedProfile = new JobProfile();
    updatedProfile.setId(jobProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(TRUE);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockJobProfilePermission()).thenReturn(false);

    // When & Then
    assertThatThrownBy(() -> jobProfileService.putJobProfile(jobProfileId, updatedProfile))
        .isInstanceOf(LockJobProfilePermissionException.class)
        .hasMessage("You do not have permission to lock this profile.");

    verify(jobProfileEntityRepository, never()).save(any());
  }

  @Test
  void shouldThrowLockJobProfilePermissionException_whenUserHasNoUnlockPermission() {
    // Given
    jobProfileEntity.setLocked(true);
    jobProfile.setLocked(true);

    JobProfile updatedProfile = new JobProfile();
    updatedProfile.setId(jobProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(FALSE);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockJobProfilePermission()).thenReturn(false);

    // When & Then
    assertThatThrownBy(() -> jobProfileService.putJobProfile(jobProfileId, updatedProfile))
        .isInstanceOf(LockJobProfilePermissionException.class)
        .hasMessage("You do not have permission to unlock this profile.");

    verify(jobProfileEntityRepository, never()).save(any());
  }

  @Test
  void shouldSetLockedAtAndLockedBy_whenLockingProfile() {
    // Given
    JobProfile updatedProfile = new JobProfile();
    updatedProfile.setId(jobProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(TRUE);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockJobProfilePermission()).thenReturn(true);
    when(jobProfileEntityRepository.save(any(JobProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    jobProfileService.putJobProfile(jobProfileId, updatedProfile);

    // Then
    verify(jobProfileEntityRepository).save(jobProfileEntityCaptor.capture());
    JobProfile savedProfile = jobProfileEntityCaptor.getValue().getJobProfile();

    assertThat(savedProfile.getLocked()).isTrue();
    assertThat(savedProfile.getLockedAt()).isNotNull().isInstanceOf(Date.class);
    assertThat(savedProfile.getLockedBy()).isEqualTo(userId);
  }

  @Test
  void shouldClearLockedAtAndLockedBy_whenUnlockingProfile() {
    // Given
    jobProfileEntity.setLocked(true);
    jobProfile.setLocked(true);
    jobProfile.setLockedAt(new Date());
    jobProfile.setLockedBy(UUID.randomUUID());

    JobProfile updatedProfile = new JobProfile();
    updatedProfile.setId(jobProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(FALSE);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockJobProfilePermission()).thenReturn(true);
    when(jobProfileEntityRepository.save(any(JobProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    jobProfileService.putJobProfile(jobProfileId, updatedProfile);

    // Then
    verify(jobProfileEntityRepository).save(jobProfileEntityCaptor.capture());
    JobProfile savedProfile = jobProfileEntityCaptor.getValue().getJobProfile();

    assertThat(savedProfile.getLocked()).isFalse();
    assertThat(savedProfile.getLockedAt()).isNull();
    assertThat(savedProfile.getLockedBy()).isNull();
  }

  @Test
  void shouldUpdateMetadataCorrectly_whenUpdatingProfile() {
    // Given
    JobProfile updatedProfile = new JobProfile();
    updatedProfile.setId(jobProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(FALSE);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(jobProfileEntityRepository.save(any(JobProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    jobProfileService.putJobProfile(jobProfileId, updatedProfile);

    // Then
    verify(jobProfileEntityRepository).save(jobProfileEntityCaptor.capture());
    JobProfile savedProfile = jobProfileEntityCaptor.getValue().getJobProfile();
    Metadata metadata = savedProfile.getMetadata();

    assertThat(metadata.getCreatedDate()).isEqualTo(jobProfile.getMetadata().getCreatedDate());
    assertThat(metadata.getUpdatedDate()).isNotNull();
    assertThat(metadata.getCreatedByUserId())
        .isEqualTo(jobProfile.getMetadata().getCreatedByUserId());
    assertThat(metadata.getUpdatedByUserId()).isEqualTo(userId.toString());
    assertThat(metadata.getCreatedByUsername())
        .isEqualTo(jobProfile.getMetadata().getCreatedByUsername());
    assertThat(metadata.getUpdatedByUsername()).isEqualTo("testuser");
  }

  @Test
  void shouldThrowDefaultJobProfileException_whenAttemptingToEditDefaultProfile() {
    // Given
    jobProfile.setDefault(TRUE);
    jobProfileEntity.setJobProfile(jobProfile);

    JobProfile updatedProfile = new JobProfile();
    updatedProfile.setId(jobProfileId);
    updatedProfile.setName("Updated Profile");

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);

    // When & Then
    assertThatThrownBy(() -> jobProfileService.putJobProfile(jobProfileId, updatedProfile))
        .isInstanceOf(DefaultJobProfileException.class)
        .hasMessage("Editing of default job profile is forbidden");

    verify(jobProfileEntityRepository, never()).save(any());
  }

  @Test
  void shouldLockNonDefaultProfile_whenChangingFromUnlockedToLocked() {
    // Given
    JobProfile updatedProfile = new JobProfile();
    updatedProfile.setId(jobProfileId);
    updatedProfile.setName("Test Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(TRUE);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockJobProfilePermission()).thenReturn(true);
    when(jobProfileEntityRepository.save(any(JobProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    jobProfileService.putJobProfile(jobProfileId, updatedProfile);

    // Then
    verify(permissionsValidator).checkLockJobProfilePermission();
    verify(jobProfileEntityRepository).save(any(JobProfileEntity.class));
  }

  @Test
  void shouldUnlockNonDefaultProfile_whenChangingFromLockedToUnlocked() {
    // Given
    jobProfileEntity.setLocked(true);
    jobProfile.setLocked(true);

    JobProfile updatedProfile = new JobProfile();
    updatedProfile.setId(jobProfileId);
    updatedProfile.setName("Test Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(FALSE);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockJobProfilePermission()).thenReturn(true);
    when(jobProfileEntityRepository.save(any(JobProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    jobProfileService.putJobProfile(jobProfileId, updatedProfile);

    // Then
    verify(permissionsValidator).checkLockJobProfilePermission();
    verify(jobProfileEntityRepository).save(any(JobProfileEntity.class));
  }

  @Test
  void shouldPreserveUserInfo_whenUpdatingProfile() {
    // Given
    JobProfile updatedProfile = new JobProfile();
    updatedProfile.setId(jobProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(FALSE);

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(jobProfileEntityRepository.save(any(JobProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    jobProfileService.putJobProfile(jobProfileId, updatedProfile);

    // Then
    verify(jobProfileEntityRepository).save(jobProfileEntityCaptor.capture());
    JobProfile savedProfile = jobProfileEntityCaptor.getValue().getJobProfile();

    assertThat(savedProfile.getUserInfo()).isNotNull();
    assertThat(savedProfile.getUserInfo().getFirstName()).isEqualTo("Test");
    assertThat(savedProfile.getUserInfo().getLastName()).isEqualTo("User");
    assertThat(savedProfile.getUserInfo().getUserName()).isEqualTo("testuser");
  }

  @Test
  void shouldHandleLockingWhenCurrentUserHasPermission() {
    // Given
    JobProfile updatedProfile = new JobProfile();
    updatedProfile.setId(jobProfileId);
    updatedProfile.setName("Profile to Lock");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(TRUE);

    UUID currentUserId = UUID.randomUUID();

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(currentUserId);
    when(userClient.getUserById(currentUserId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockJobProfilePermission()).thenReturn(true);
    when(jobProfileEntityRepository.save(any(JobProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    jobProfileService.putJobProfile(jobProfileId, updatedProfile);

    // Then
    verify(jobProfileEntityRepository).save(jobProfileEntityCaptor.capture());
    JobProfile savedProfile = jobProfileEntityCaptor.getValue().getJobProfile();

    assertThat(savedProfile.getLocked()).isTrue();
    assertThat(savedProfile.getLockedBy()).isEqualTo(currentUserId);
  }

  @Test
  void shouldHandleUnlockingWhenCurrentUserHasPermission() {
    // Given
    UUID originalLockingUserId = UUID.randomUUID();
    jobProfileEntity.setLocked(true);
    jobProfile.setLocked(true);
    jobProfile.setLockedBy(originalLockingUserId);
    jobProfile.setLockedAt(new Date());

    JobProfile updatedProfile = new JobProfile();
    updatedProfile.setId(jobProfileId);
    updatedProfile.setName("Profile to Unlock");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(FALSE);

    UUID currentUserId = UUID.randomUUID();

    when(jobProfileEntityRepository.getReferenceById(jobProfileId)).thenReturn(jobProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(currentUserId);
    when(userClient.getUserById(currentUserId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockJobProfilePermission()).thenReturn(true);
    when(jobProfileEntityRepository.save(any(JobProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    jobProfileService.putJobProfile(jobProfileId, updatedProfile);

    // Then
    verify(jobProfileEntityRepository).save(jobProfileEntityCaptor.capture());
    JobProfile savedProfile = jobProfileEntityCaptor.getValue().getJobProfile();

    assertThat(savedProfile.getLocked()).isFalse();
    assertThat(savedProfile.getLockedBy()).isNull();
    assertThat(savedProfile.getLockedAt()).isNull();
  }

  @Test
  @TestMate(name = "TestMate-029c7d5c8c6c036fad395b01ed77f442")
  void postJobProfileShouldLockProfileWhenLockedIsTrueAndPermissionExists() {
    // Given
    jobProfile.setLocked(TRUE);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockJobProfilePermission()).thenReturn(true);
    when(jobProfileEntityRepository.save(any(JobProfileEntity.class)))
        .thenAnswer(
            invocation -> {
              // Simulate DB saving and returning the saved entity
              return invocation.getArgument(0);
            });
    // When
    JobProfile savedProfile = jobProfileService.postJobProfile(jobProfile);
    // Then
    verify(permissionsValidator).checkLockJobProfilePermission();
    verify(jobProfileEntityRepository).save(jobProfileEntityCaptor.capture());
    JobProfile capturedProfile = jobProfileEntityCaptor.getValue().getJobProfile();
    assertThat(savedProfile).isSameAs(capturedProfile);
    assertThat(savedProfile.getLocked()).isTrue();
    assertThat(savedProfile.getLockedBy()).isEqualTo(userId);
    assertThat(savedProfile.getLockedAt()).isNotNull();
    Metadata metadata = savedProfile.getMetadata();
    assertThat(metadata.getCreatedByUserId()).isEqualTo(userId.toString());
    assertThat(metadata.getUpdatedByUserId()).isEqualTo(userId.toString());
    UserInfo userInfo = savedProfile.getUserInfo();
    assertThat(userInfo.getFirstName()).isEqualTo("Test");
    assertThat(userInfo.getLastName()).isEqualTo("User");
    assertThat(userInfo.getUserName()).isEqualTo("testuser");
  }

  @Test
  @TestMate(name = "TestMate-cb876ac745b745330a34d6ebf401dcba")
  void postJobProfileShouldThrowExceptionWhenLockingAndPermissionIsMissing() {
    // Given
    jobProfile.setLocked(TRUE);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockJobProfilePermission()).thenReturn(false);
    // When & Then
    assertThatThrownBy(() -> jobProfileService.postJobProfile(jobProfile))
        .isInstanceOf(LockJobProfilePermissionException.class)
        .hasMessage("You do not have permission to lock this profile.");
    verify(permissionsValidator).checkLockJobProfilePermission();
    verify(jobProfileEntityRepository, never()).save(any(JobProfileEntity.class));
  }

  @Test
  void getJobProfilesShouldReturnUsedJobProfilesWhenUsedIsTrue() {
    // TestMate-09489a728d9d6346b40fd70f88cf5915
    // Given
    var offset = 0;
    var limit = 10;
    var used = TRUE;
    var query = (String) null;
    var profileId = UUID.fromString("c85d533c-a043-465c-a532-d62101086611");
    var profileName = "Used Profile Name";
    Object[] profileData = new Object[] {profileId, profileName};
    List<Object[]> mockListData = Collections.singletonList(profileData);
    when(jobProfileEntityCqlRepository.getUsedJobProfilesData(offset, limit))
        .thenReturn(mockListData);
    // When
    JobProfileCollection result = jobProfileService.getJobProfiles(used, query, offset, limit);
    // Then
    assertThat(result.getTotalRecords()).isEqualTo(1);
    assertThat(result.getJobProfiles()).hasSize(1);
    assertThat(result.getJobProfiles().get(0).getId()).isEqualTo(profileId);
    assertThat(result.getJobProfiles().get(0).getName()).isEqualTo(profileName);
    verify(jobProfileEntityCqlRepository).getUsedJobProfilesData(offset, limit);
  }

  @Test
  void getJobProfilesShouldReturnAllProfilesWhenUsedIsFalseAndQueryIsEmpty() {
    // TestMate-16400b16bf1a763aab0f8854d5c4d9f6
    // Given
    var used = FALSE;
    var query = "";
    var offset = 0;
    var limit = 20;
    var defaultCql = "(cql.allRecords=1)";
    var id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var profile1 = new JobProfile().id(id1).name("Profile 1");
    var profile2 = new JobProfile().id(id2).name("Profile 2");
    var entity1 = JobProfileEntity.builder().id(id1).jobProfile(profile1).build();
    var entity2 = JobProfileEntity.builder().id(id2).jobProfile(profile2).build();
    var page = new PageImpl<>(List.of(entity1, entity2));
    var offsetRequest = OffsetRequest.of(offset, limit);
    when(jobProfileEntityCqlRepository.findByCql(defaultCql, offsetRequest)).thenReturn(page);
    // When
    JobProfileCollection result = jobProfileService.getJobProfiles(used, query, offset, limit);
    // Then
    assertThat(result.getJobProfiles()).hasSize(2);
    assertThat(result.getTotalRecords()).isEqualTo(2);
    assertThat(result.getJobProfiles().get(0).getId()).isEqualTo(id1);
    assertThat(result.getJobProfiles().get(0).getName()).isEqualTo("Profile 1");
    assertThat(result.getJobProfiles().get(1).getId()).isEqualTo(id2);
    assertThat(result.getJobProfiles().get(1).getName()).isEqualTo("Profile 2");
    verify(jobProfileEntityCqlRepository).findByCql(defaultCql, offsetRequest);
  }

  @Test
  @TestMate(name = "TestMate-173165a4f1cbf3bf1a9d77c49a292718")
  void getJobProfilesShouldReturnFilteredProfilesWhenQueryIsProvided() {
    // Given
    var query = "name==\"Export Profile\"";
    var offset = 0;
    var limit = 10;
    var profileId = UUID.fromString("c85d533c-a043-465c-a532-d62101086611");
    var profileName = "Export Profile";
    var entity =
        JobProfileEntity.builder()
            .id(profileId)
            .jobProfile(new JobProfile().id(profileId).name(profileName))
            .build();
    var page = new PageImpl<>(List.of(entity));
    var offsetRequest = OffsetRequest.of(offset, limit);
    when(jobProfileEntityCqlRepository.findByCql(query, offsetRequest)).thenReturn(page);
    // When
    JobProfileCollection result = jobProfileService.getJobProfiles(FALSE, query, offset, limit);
    // Then
    verify(jobProfileEntityCqlRepository).findByCql(query, offsetRequest);
    assertThat(result.getTotalRecords()).isEqualTo(1);
    assertThat(result.getJobProfiles()).hasSize(1);
    assertThat(result.getJobProfiles().get(0).getId()).isEqualTo(profileId);
    assertThat(result.getJobProfiles().get(0).getName()).isEqualTo(profileName);
  }

  @Test
  @TestMate(name = "TestMate-55d3d2beb07bac5b2aea7b654574fea3")
  void getJobProfilesShouldHandleEmptyResultsFromCqlRepository() {
    // Given
    var used = FALSE;
    var query = "name==nonexistent";
    var offset = 0;
    var limit = 10;
    var offsetRequest = OffsetRequest.of(offset, limit);
    when(jobProfileEntityCqlRepository.findByCql(query, offsetRequest))
        .thenReturn(Page.empty(offsetRequest));
    // When
    JobProfileCollection result = jobProfileService.getJobProfiles(used, query, offset, limit);
    // Then
    assertThat(result.getJobProfiles()).isEmpty();
    assertThat(result.getTotalRecords()).isZero();
    verify(jobProfileEntityCqlRepository).findByCql(query, offsetRequest);
  }

  // Helper methods

  private JobExecution createJobExecution(UUID id, String profileName, UUID profileId) {
    JobExecution jobExecution = new JobExecution();
    jobExecution.setId(id);
    jobExecution.setJobProfileName(profileName);
    jobExecution.setJobProfileId(profileId);
    jobExecution.setExportedFiles(new HashSet<>());
    return jobExecution;
  }

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
