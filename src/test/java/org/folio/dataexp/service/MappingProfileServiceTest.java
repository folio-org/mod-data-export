package org.folio.dataexp.service;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.JobProfileCollection;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.domain.dto.User;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.exception.mapping.profile.DefaultMappingProfileException;
import org.folio.dataexp.exception.mapping.profile.LockMappingProfileException;
import org.folio.dataexp.exception.mapping.profile.LockMappingProfilePermissionException;
import org.folio.dataexp.repository.MappingProfileEntityCqlRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.service.validators.MappingProfileValidator;
import org.folio.dataexp.service.validators.PermissionsValidator;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class MappingProfileServiceTest {
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private MappingProfileEntityRepository mappingProfileEntityRepository;
  @Mock private MappingProfileEntityCqlRepository mappingProfileEntityCqlRepository;
  @Mock private MappingProfileValidator mappingProfileValidator;
  @Mock private UserClient userClient;
  @Mock private PermissionsValidator permissionsValidator;
  @Mock private JobProfileService jobProfileService;

  @InjectMocks private MappingProfileService mappingProfileService;

  @Captor private ArgumentCaptor<MappingProfileEntity> mappingProfileEntityCaptor;

  private UUID mappingProfileId;
  private UUID userId;
  private MappingProfile mappingProfile;
  private MappingProfileEntity mappingProfileEntity;
  private User user;

  @BeforeEach
  void setUp() {
    mappingProfileId = UUID.randomUUID();
    userId = UUID.randomUUID();

    var metadata = new Metadata();
    metadata.setCreatedDate(new Date());
    metadata.setUpdatedDate(new Date());
    metadata.setCreatedByUserId(userId.toString());
    metadata.setUpdatedByUserId(userId.toString());
    metadata.setCreatedByUsername("testuser");
    metadata.setUpdatedByUsername("testuser");

    mappingProfile = new MappingProfile();
    mappingProfile.setId(mappingProfileId);
    mappingProfile.setName("Test Profile");
    mappingProfile.setDefault(FALSE);
    mappingProfile.setLocked(FALSE);
    mappingProfile.setMetadata(metadata);

    mappingProfileEntity = new MappingProfileEntity();
    mappingProfileEntity.setId(mappingProfileId);
    mappingProfileEntity.setMappingProfile(mappingProfile);
    mappingProfileEntity.setLocked(false);

    user = new User();
    user.setId(userId.toString());
    user.setUsername("testuser");
    var personal = new User.Personal();
    personal.setFirstName("Test");
    personal.setLastName("User");
    user.setPersonal(personal);
  }

  // ========== Tests for deleteMappingProfileById method ==========

  @Test
  void deleteMappingProfileByIdTest() {
    var profile = new MappingProfile();
    profile.setId(UUID.randomUUID());
    profile.setDefault(false);

    user = new User();
    user.setPersonal(new User.Personal());
    user.setUsername("testuser");
    profile.setName("mappingProfile");
    var entity = MappingProfileEntity.builder().id(profile.getId()).mappingProfile(profile).build();

    var emptyJobProfileCollection = new JobProfileCollection();
    emptyJobProfileCollection.setJobProfiles(List.of());
    emptyJobProfileCollection.setTotalRecords(0);

    when(mappingProfileEntityRepository.getReferenceById(profile.getId()))
        .thenReturn(entity);
    when(jobProfileService.getJobProfiles(any(), any(), any(), any()))
        .thenReturn(emptyJobProfileCollection);

    mappingProfileService.deleteMappingProfileById(profile.getId());

    // Then
    verify(mappingProfileEntityRepository).getReferenceById(profile.getId());
    verify(jobProfileService)
        .getJobProfiles(
            null, "(mappingProfileId=%s)".formatted(profile.getId()), 0, Integer.MAX_VALUE);
    verify(mappingProfileEntityRepository).deleteById(profile.getId());
  }

  @Test
  void deleteMappingProfileById_shouldThrowDefaultMappingProfileException_whenProfileIsDefault() {
    // Given
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("defaultMappingProfile");

    var entity =
        MappingProfileEntity.builder()
            .id(mappingProfile.getId())
            .mappingProfile(mappingProfile)
            .locked(false)
            .build();

    when(mappingProfileEntityRepository.getReferenceById(mappingProfile.getId()))
        .thenReturn(entity);

    // When & Then
    assertThatThrownBy(() -> mappingProfileService.deleteMappingProfileById(mappingProfile.getId()))
        .isInstanceOf(DefaultMappingProfileException.class)
        .hasMessage("Deletion of default mapping profile is forbidden");

    verify(mappingProfileEntityRepository).getReferenceById(mappingProfile.getId());
    verify(jobProfileService, never()).getJobProfiles(any(), any(), any(), any());
    verify(mappingProfileEntityRepository, never()).deleteById(any());
  }

  @Test
  void deleteMappingProfileById_shouldThrowLockMappingProfileException_whenProfileIsLocked() {
    // Given
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(false);
    mappingProfile.setName("lockedMappingProfile");

    var entity =
        MappingProfileEntity.builder()
            .id(mappingProfile.getId())
            .mappingProfile(mappingProfile)
            .locked(true)
            .build();

    when(mappingProfileEntityRepository.getReferenceById(mappingProfile.getId()))
        .thenReturn(entity);

    // When & Then
    assertThatThrownBy(() -> mappingProfileService.deleteMappingProfileById(mappingProfile.getId()))
        .isInstanceOf(LockMappingProfileException.class)
        .hasMessage(
            "This profile is locked. Please unlock the profile to proceed with editing/deletion.");

    verify(mappingProfileEntityRepository).getReferenceById(mappingProfile.getId());
    verify(jobProfileService, never()).getJobProfiles(any(), any(), any(), any());
    verify(mappingProfileEntityRepository, never()).deleteById(any());
  }

  @Test
  void deleteById_shouldThrowLockMappingProfileException_whenProfileIsLinkedToJobProfiles() {
    // Given
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(false);
    mappingProfile.setName("linkedMappingProfile");

    var entity =
        MappingProfileEntity.builder()
            .id(mappingProfile.getId())
            .mappingProfile(mappingProfile)
            .locked(false)
            .build();

    UUID jobProfile1Id = UUID.randomUUID();
    UUID jobProfile2Id = UUID.randomUUID();

    var jobProfile1 = new JobProfile();
    jobProfile1.setId(jobProfile1Id);
    jobProfile1.setName("Job Profile 1");

    var jobProfile2 = new JobProfile();
    jobProfile2.setId(jobProfile2Id);
    jobProfile2.setName("Job Profile 2");

    var jobProfileCollection = new JobProfileCollection();
    jobProfileCollection.setJobProfiles(List.of(jobProfile1, jobProfile2));
    jobProfileCollection.setTotalRecords(2);

    when(mappingProfileEntityRepository.getReferenceById(mappingProfile.getId()))
        .thenReturn(entity);
    when(jobProfileService.getJobProfiles(
            null, "(mappingProfileId=%s)".formatted(mappingProfile.getId()), 0, Integer.MAX_VALUE))
        .thenReturn(jobProfileCollection);

    // When & Then
    assertThatThrownBy(() -> mappingProfileService.deleteMappingProfileById(mappingProfile.getId()))
        .isInstanceOf(LockMappingProfileException.class)
        .hasMessageContaining("Cannot delete mapping profile linked to job profiles:")
        .hasMessageContaining(jobProfile1Id.toString())
        .hasMessageContaining(jobProfile2Id.toString());

    verify(mappingProfileEntityRepository).getReferenceById(mappingProfile.getId());
    verify(jobProfileService)
        .getJobProfiles(
            null, "(mappingProfileId=%s)".formatted(mappingProfile.getId()), 0, Integer.MAX_VALUE);
    verify(mappingProfileEntityRepository, never()).deleteById(any());
  }

  @Test
  void deleteById_shouldThrowLockMappingProfileException_whenProfileIsLinkedToSingleJobProfile() {
    // Given
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(false);
    mappingProfile.setName("linkedMappingProfile");

    var entity =
        MappingProfileEntity.builder()
            .id(mappingProfile.getId())
            .mappingProfile(mappingProfile)
            .locked(false)
            .build();

    UUID jobProfileId = UUID.randomUUID();
    var jobProfile = new JobProfile();
    jobProfile.setId(jobProfileId);
    jobProfile.setName("Single Job Profile");

    var jobProfileCollection = new JobProfileCollection();
    jobProfileCollection.setJobProfiles(List.of(jobProfile));
    jobProfileCollection.setTotalRecords(1);

    when(mappingProfileEntityRepository.getReferenceById(mappingProfile.getId()))
        .thenReturn(entity);
    when(jobProfileService.getJobProfiles(
            null, "(mappingProfileId=%s)".formatted(mappingProfile.getId()), 0, Integer.MAX_VALUE))
        .thenReturn(jobProfileCollection);

    // When & Then
    assertThatThrownBy(() -> mappingProfileService.deleteMappingProfileById(mappingProfile.getId()))
        .isInstanceOf(LockMappingProfileException.class)
        .hasMessageContaining("Cannot delete mapping profile linked to job profiles:")
        .hasMessageContaining(jobProfileId.toString());

    verify(mappingProfileEntityRepository).getReferenceById(mappingProfile.getId());
    verify(jobProfileService)
        .getJobProfiles(
            null, "(mappingProfileId=%s)".formatted(mappingProfile.getId()), 0, Integer.MAX_VALUE);
    verify(mappingProfileEntityRepository, never()).deleteById(any());
  }

  // ========== End of deleteMappingProfileById tests ==========

  @Test
  @SneakyThrows
  void getMappingProfileByIdTest() {
    var profile = new MappingProfile();
    profile.setId(UUID.randomUUID());
    profile.setDefault(true);
    profile.setName("mappingProfile");

    var entity = MappingProfileEntity.builder().id(profile.getId()).mappingProfile(profile).build();
    when(mappingProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);

    mappingProfileService.getMappingProfileById(profile.getId());

    verify(mappingProfileEntityRepository).getReferenceById(isA(UUID.class));
  }

  @Test
  @SneakyThrows
  void getMappingProfiles() {
    var profile = new MappingProfile();
    profile.setId(UUID.randomUUID());
    profile.setDefault(true);
    profile.setName("mappingProfile");

    var entity = MappingProfileEntity.builder().id(profile.getId()).mappingProfile(profile).build();
    PageImpl<MappingProfileEntity> page = new PageImpl<>(List.of(entity));

    when(mappingProfileEntityCqlRepository.findByCql(isA(String.class), isA(OffsetRequest.class)))
        .thenReturn(page);

    mappingProfileService.getMappingProfiles("query", 2, 1);

    verify(mappingProfileEntityCqlRepository)
        .findByCql(isA(String.class), isA(OffsetRequest.class));
  }

  @Test
  @SneakyThrows
  void postMappingProfileTest() {
    var transformation = new Transformations();
    transformation.setFieldId("holdings.callnumber");
    transformation.setPath("$.holdings[*].callNumber");
    transformation.setRecordType(RecordTypes.HOLDINGS);
    transformation.setTransformation("900  $a");
    var profile = new MappingProfile();
    profile.setId(UUID.randomUUID());
    profile.setDefault(true);
    profile.setName("mappingProfile");
    profile.setTransformations(List.of(transformation));
    var userDto = new User();
    userDto.setPersonal(new User.Personal());
    var entity = MappingProfileEntity.builder().id(profile.getId()).mappingProfile(profile).build();

    when(mappingProfileEntityRepository.save(isA(MappingProfileEntity.class))).thenReturn(entity);
    when(userClient.getUserById(isA(String.class))).thenReturn(userDto);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    mappingProfileService.postMappingProfile(profile);

    verify(mappingProfileEntityRepository).save(isA(MappingProfileEntity.class));
    verify(mappingProfileValidator).validate(isA(MappingProfile.class));
  }

  @Test
  void putMappingProfileTest() {
    var profile = new MappingProfile();
    profile.setId(UUID.randomUUID());
    profile.setDefault(false);
    profile.setName("mappingProfile");
    profile.setMetadata(new Metadata().createdDate(new Date()));
    var userDto = new User();
    userDto.setPersonal(new User.Personal());

    var entity = MappingProfileEntity.builder().id(profile.getId()).mappingProfile(profile).build();
    when(mappingProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);
    when(mappingProfileEntityRepository.save(isA(MappingProfileEntity.class))).thenReturn(entity);
    when(userClient.getUserById(isA(String.class))).thenReturn(userDto);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    mappingProfileService.putMappingProfile(profile.getId(), profile);

    verify(mappingProfileEntityRepository).save(isA(MappingProfileEntity.class));
    verify(mappingProfileValidator).validate(isA(MappingProfile.class));
  }

  // ========== Tests for putMappingProfile with updateLock, lockProfile, and unlockProfile

  @Test
  void shouldLockProfile_whenLockStatusChangesFromFalseToTrue() {
    // Given
    MappingProfile updatedProfile = new MappingProfile();
    updatedProfile.setId(mappingProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(TRUE);
    updatedProfile.setMetadata(new Metadata().createdDate(new Date()));

    user = new User();
    user.setPersonal(new User.Personal());
    user.setUsername("testuser");

    mappingProfileEntity =
        MappingProfileEntity.builder().id(mappingProfileId).mappingProfile(mappingProfile).build();

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockMappingProfilePermission()).thenReturn(true);
    when(mappingProfileEntityRepository.save(isA(MappingProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    mappingProfileService.putMappingProfile(mappingProfileId, updatedProfile);

    // Then
    verify(permissionsValidator).checkLockMappingProfilePermission();
    verify(mappingProfileEntityRepository).save(mappingProfileEntityCaptor.capture());

    MappingProfile savedProfile = mappingProfileEntityCaptor.getValue().getMappingProfile();
    assertThat(savedProfile.getLocked()).isTrue();
    assertThat(savedProfile.getLockedAt()).isNotNull();
    assertThat(savedProfile.getLockedBy()).isEqualTo(userId);
  }

  @Test
  void shouldUnlockProfile_whenLockStatusChangesFromTrueToFalse() {
    // Given
    mappingProfile.setLocked(true);
    mappingProfile.setMetadata(new Metadata().createdDate(new Date()));

    mappingProfileEntity =
        MappingProfileEntity.builder()
            .id(mappingProfileId)
            .mappingProfile(mappingProfile)
            .locked(true)
            .build();

    MappingProfile updatedProfile = new MappingProfile();
    updatedProfile.setId(mappingProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(FALSE);
    updatedProfile.setMetadata(new Metadata().createdDate(new Date()));

    user = new User();
    user.setPersonal(new User.Personal());
    user.setUsername("testuser");

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockMappingProfilePermission()).thenReturn(true);
    when(mappingProfileEntityRepository.save(isA(MappingProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    mappingProfileService.putMappingProfile(mappingProfileId, updatedProfile);

    // Then
    verify(permissionsValidator).checkLockMappingProfilePermission();
    verify(mappingProfileEntityRepository).save(mappingProfileEntityCaptor.capture());

    MappingProfile savedProfile = mappingProfileEntityCaptor.getValue().getMappingProfile();
    assertThat(savedProfile.getLocked()).isFalse();
    assertThat(savedProfile.getLockedAt()).isNull();
    assertThat(savedProfile.getLockedBy()).isNull();
  }

  @Test
  void shouldNotChangeLock_whenLockStatusRemainsUnchanged() {
    // Given
    MappingProfile updatedProfile = new MappingProfile();
    updatedProfile.setId(mappingProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(FALSE);
    updatedProfile.setMetadata(new Metadata().createdDate(new Date()));

    user = new User();
    user.setPersonal(new User.Personal());
    user.setUsername("testuser");

    mappingProfileEntity =
        MappingProfileEntity.builder().id(mappingProfileId).mappingProfile(mappingProfile).build();

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(mappingProfileEntityRepository.save(any(MappingProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    mappingProfileService.putMappingProfile(mappingProfileId, updatedProfile);

    // Then
    verify(permissionsValidator, never()).checkLockMappingProfilePermission();
    verify(permissionsValidator, never()).checkLockMappingProfilePermission();
  }

  @Test
  void shouldThrowLockMappingProfilePermissionException_whenUserHasNoLockPermission() {
    // Given
    MappingProfile updatedProfile = new MappingProfile();
    updatedProfile.setId(mappingProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(TRUE);
    updatedProfile.setMetadata(new Metadata().createdDate(new Date()));

    user = new User();
    user.setPersonal(new User.Personal());
    user.setUsername("testuser");

    mappingProfileEntity =
        MappingProfileEntity.builder().id(mappingProfileId).mappingProfile(mappingProfile).build();

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockMappingProfilePermission()).thenReturn(false);

    // When & Then
    assertThatThrownBy(
            () -> mappingProfileService.putMappingProfile(mappingProfileId, updatedProfile))
        .isInstanceOf(LockMappingProfilePermissionException.class)
        .hasMessage("You do not have permission to lock this profile.");

    verify(mappingProfileEntityRepository, never()).save(any());
  }

  @Test
  void shouldThrowLockMappingProfilePermissionException_whenUserHasNoUnlockPermission() {
    // Given
    mappingProfile.setLocked(true);
    mappingProfile.setMetadata(new Metadata().createdDate(new Date()));

    mappingProfileEntity =
        MappingProfileEntity.builder()
            .id(mappingProfileId)
            .mappingProfile(mappingProfile)
            .locked(true)
            .build();

    MappingProfile updatedProfile = new MappingProfile();
    updatedProfile.setId(mappingProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(FALSE);
    updatedProfile.setMetadata(new Metadata().createdDate(new Date()));

    user = new User();
    user.setPersonal(new User.Personal());
    user.setUsername("testuser");

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockMappingProfilePermission()).thenReturn(false);

    // When & Then
    assertThatThrownBy(
            () -> mappingProfileService.putMappingProfile(mappingProfileId, updatedProfile))
        .isInstanceOf(LockMappingProfilePermissionException.class)
        .hasMessage("You do not have permission to unlock this profile.");

    verify(mappingProfileEntityRepository, never()).save(any());
  }

  @Test
  void shouldSetLockedAtAndLockedBy_whenLockingProfile() {
    // Given
    MappingProfile updatedProfile = new MappingProfile();
    updatedProfile.setId(mappingProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(TRUE);
    updatedProfile.setMetadata(new Metadata().createdDate(new Date()));

    user = new User();
    user.setPersonal(new User.Personal());
    user.setUsername("testuser");

    mappingProfileEntity =
        MappingProfileEntity.builder().id(mappingProfileId).mappingProfile(mappingProfile).build();

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockMappingProfilePermission()).thenReturn(true);
    when(mappingProfileEntityRepository.save(any(MappingProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    mappingProfileService.putMappingProfile(mappingProfileId, updatedProfile);

    // Then
    verify(mappingProfileEntityRepository).save(mappingProfileEntityCaptor.capture());
    MappingProfile savedProfile = mappingProfileEntityCaptor.getValue().getMappingProfile();

    assertThat(savedProfile.getLocked()).isTrue();
    assertThat(savedProfile.getLockedAt()).isNotNull().isInstanceOf(Date.class);
    assertThat(savedProfile.getLockedBy()).isEqualTo(userId);
  }

  @Test
  void shouldClearLockedAtAndLockedBy_whenUnlockingProfile() {
    // Given
    mappingProfile.setLocked(true);
    mappingProfile.setLockedAt(new Date());
    mappingProfile.setLockedBy(UUID.randomUUID());
    mappingProfile.setMetadata(new Metadata().createdDate(new Date()));

    mappingProfileEntity =
        MappingProfileEntity.builder()
            .id(mappingProfileId)
            .mappingProfile(mappingProfile)
            .locked(true)
            .build();

    MappingProfile updatedProfile = new MappingProfile();
    updatedProfile.setId(mappingProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(FALSE);
    updatedProfile.setMetadata(new Metadata().createdDate(new Date()));

    user = new User();
    user.setPersonal(new User.Personal());
    user.setUsername("testuser");

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockMappingProfilePermission()).thenReturn(true);
    when(mappingProfileEntityRepository.save(any(MappingProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    mappingProfileService.putMappingProfile(mappingProfileId, updatedProfile);

    // Then
    verify(mappingProfileEntityRepository).save(mappingProfileEntityCaptor.capture());
    MappingProfile savedProfile = mappingProfileEntityCaptor.getValue().getMappingProfile();

    assertThat(savedProfile.getLocked()).isFalse();
    assertThat(savedProfile.getLockedAt()).isNull();
    assertThat(savedProfile.getLockedBy()).isNull();
  }

  @Test
  void shouldUpdateMetadataCorrectly_whenUpdatingProfile() {
    // Given
    MappingProfile updatedProfile = new MappingProfile();
    updatedProfile.setId(mappingProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(FALSE);
    updatedProfile.setMetadata(new Metadata().createdDate(new Date()));

    user = new User();
    user.setPersonal(new User.Personal());
    user.setUsername("testuser");

    mappingProfile.setMetadata(
        new Metadata()
            .createdDate(new Date())
            .createdByUserId(UUID.randomUUID().toString())
            .createdByUsername("originaluser"));

    mappingProfileEntity =
        MappingProfileEntity.builder().id(mappingProfileId).mappingProfile(mappingProfile).build();

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(mappingProfileEntityRepository.save(any(MappingProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    mappingProfileService.putMappingProfile(mappingProfileId, updatedProfile);

    // Then
    verify(mappingProfileEntityRepository).save(mappingProfileEntityCaptor.capture());
    MappingProfile savedProfile = mappingProfileEntityCaptor.getValue().getMappingProfile();
    Metadata metadata = savedProfile.getMetadata();

    assertThat(metadata.getCreatedDate()).isEqualTo(mappingProfile.getMetadata().getCreatedDate());
    assertThat(metadata.getUpdatedDate()).isNotNull();
    assertThat(metadata.getCreatedByUserId())
        .isEqualTo(mappingProfile.getMetadata().getCreatedByUserId());
    assertThat(metadata.getUpdatedByUserId()).isEqualTo(userId.toString());
    assertThat(metadata.getCreatedByUsername())
        .isEqualTo(mappingProfile.getMetadata().getCreatedByUsername());
    assertThat(metadata.getUpdatedByUsername()).isEqualTo("testuser");
  }

  @Test
  void shouldThrowDefaultMappingProfileException_whenAttemptingToEditDefaultProfile() {
    // Given
    mappingProfile.setDefault(TRUE);
    mappingProfile.setMetadata(new Metadata().createdDate(new Date()));
    mappingProfileEntity =
        MappingProfileEntity.builder().id(mappingProfileId).mappingProfile(mappingProfile).build();

    MappingProfile updatedProfile = new MappingProfile();
    updatedProfile.setId(mappingProfileId);
    updatedProfile.setName("Updated Profile");

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);

    // When & Then
    assertThatThrownBy(
            () -> mappingProfileService.putMappingProfile(mappingProfileId, updatedProfile))
        .isInstanceOf(
            org.folio.dataexp.exception.mapping.profile.DefaultMappingProfileException.class)
        .hasMessage("Editing of default mapping profile is forbidden");

    verify(mappingProfileEntityRepository, never()).save(any());
  }

  @Test
  void shouldLockNonDefaultProfile_whenChangingFromUnlockedToLocked() {
    // Given
    MappingProfile updatedProfile = new MappingProfile();
    updatedProfile.setId(mappingProfileId);
    updatedProfile.setName("Test Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(TRUE);
    updatedProfile.setMetadata(new Metadata().createdDate(new Date()));

    user = new User();
    user.setPersonal(new User.Personal());
    user.setUsername("testuser");

    mappingProfile.setMetadata(new Metadata().createdDate(new Date()));
    mappingProfileEntity =
        MappingProfileEntity.builder().id(mappingProfileId).mappingProfile(mappingProfile).build();

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockMappingProfilePermission()).thenReturn(true);
    when(mappingProfileEntityRepository.save(any(MappingProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    mappingProfileService.putMappingProfile(mappingProfileId, updatedProfile);

    // Then
    verify(permissionsValidator).checkLockMappingProfilePermission();
    verify(mappingProfileEntityRepository).save(any(MappingProfileEntity.class));
  }

  @Test
  void shouldUnlockNonDefaultProfile_whenChangingFromLockedToUnlocked() {
    // Given
    mappingProfile.setLocked(true);
    mappingProfile.setMetadata(new Metadata().createdDate(new Date()));

    mappingProfileEntity =
        MappingProfileEntity.builder()
            .id(mappingProfileId)
            .mappingProfile(mappingProfile)
            .locked(true)
            .build();

    MappingProfile updatedProfile = new MappingProfile();
    updatedProfile.setId(mappingProfileId);
    updatedProfile.setName("Test Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(FALSE);
    updatedProfile.setMetadata(new Metadata().createdDate(new Date()));

    user = new User();
    user.setPersonal(new User.Personal());
    user.setUsername("testuser");

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockMappingProfilePermission()).thenReturn(true);
    when(mappingProfileEntityRepository.save(any(MappingProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    mappingProfileService.putMappingProfile(mappingProfileId, updatedProfile);

    // Then
    verify(permissionsValidator).checkLockMappingProfilePermission();
    verify(mappingProfileEntityRepository).save(any(MappingProfileEntity.class));
  }

  @Test
  void shouldPreserveUserInfo_whenUpdatingProfile() {
    // Given
    MappingProfile updatedProfile = new MappingProfile();
    updatedProfile.setId(mappingProfileId);
    updatedProfile.setName("Updated Profile");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(FALSE);
    updatedProfile.setMetadata(new Metadata().createdDate(new Date()));

    user = new User();
    User.Personal personal = new User.Personal();
    personal.setFirstName("Test");
    personal.setLastName("User");
    user.setPersonal(personal);
    user.setUsername("testuser");

    mappingProfile.setMetadata(new Metadata().createdDate(new Date()));
    mappingProfileEntity =
        MappingProfileEntity.builder().id(mappingProfileId).mappingProfile(mappingProfile).build();

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(userClient.getUserById(userId.toString())).thenReturn(user);
    when(mappingProfileEntityRepository.save(any(MappingProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    mappingProfileService.putMappingProfile(mappingProfileId, updatedProfile);

    // Then
    verify(mappingProfileEntityRepository).save(mappingProfileEntityCaptor.capture());
    MappingProfile savedProfile = mappingProfileEntityCaptor.getValue().getMappingProfile();

    assertThat(savedProfile.getUserInfo()).isNotNull();
    assertThat(savedProfile.getUserInfo().getFirstName()).isEqualTo("Test");
    assertThat(savedProfile.getUserInfo().getLastName()).isEqualTo("User");
    assertThat(savedProfile.getUserInfo().getUserName()).isEqualTo("testuser");
  }

  @Test
  void shouldHandleLockingWhenCurrentUserHasPermission() {
    // Given
    MappingProfile updatedProfile = new MappingProfile();
    updatedProfile.setId(mappingProfileId);
    updatedProfile.setName("Profile to Lock");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(TRUE);
    updatedProfile.setMetadata(new Metadata().createdDate(new Date()));

    UUID currentUserId = UUID.randomUUID();

    user = new User();
    user.setPersonal(new User.Personal());
    user.setUsername("testuser");

    mappingProfile.setMetadata(new Metadata().createdDate(new Date()));
    mappingProfileEntity =
        MappingProfileEntity.builder().id(mappingProfileId).mappingProfile(mappingProfile).build();

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(currentUserId);
    when(userClient.getUserById(currentUserId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockMappingProfilePermission()).thenReturn(true);
    when(mappingProfileEntityRepository.save(any(MappingProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    mappingProfileService.putMappingProfile(mappingProfileId, updatedProfile);

    // Then
    verify(mappingProfileEntityRepository).save(mappingProfileEntityCaptor.capture());
    MappingProfile savedProfile = mappingProfileEntityCaptor.getValue().getMappingProfile();

    assertThat(savedProfile.getLocked()).isTrue();
    assertThat(savedProfile.getLockedBy()).isEqualTo(currentUserId);
  }

  @Test
  void shouldHandleUnlockingWhenCurrentUserHasPermission() {
    // Given
    UUID originalLockingUserId = UUID.randomUUID();
    mappingProfile.setLocked(true);
    mappingProfile.setLockedBy(originalLockingUserId);
    mappingProfile.setLockedAt(new Date());
    mappingProfile.setMetadata(new Metadata().createdDate(new Date()));

    mappingProfileEntity =
        MappingProfileEntity.builder()
            .id(mappingProfileId)
            .mappingProfile(mappingProfile)
            .locked(true)
            .build();

    MappingProfile updatedProfile = new MappingProfile();
    updatedProfile.setId(mappingProfileId);
    updatedProfile.setName("Profile to Unlock");
    updatedProfile.setDefault(FALSE);
    updatedProfile.setLocked(FALSE);
    updatedProfile.setMetadata(new Metadata().createdDate(new Date()));

    UUID currentUserId = UUID.randomUUID();

    user = new User();
    user.setPersonal(new User.Personal());
    user.setUsername("testuser");

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(currentUserId);
    when(userClient.getUserById(currentUserId.toString())).thenReturn(user);
    when(permissionsValidator.checkLockMappingProfilePermission()).thenReturn(true);
    when(mappingProfileEntityRepository.save(any(MappingProfileEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    mappingProfileService.putMappingProfile(mappingProfileId, updatedProfile);

    // Then
    verify(mappingProfileEntityRepository).save(mappingProfileEntityCaptor.capture());
    MappingProfile savedProfile = mappingProfileEntityCaptor.getValue().getMappingProfile();

    assertThat(savedProfile.getLocked()).isFalse();
    assertThat(savedProfile.getLockedBy()).isNull();
    assertThat(savedProfile.getLockedAt()).isNull();
  }
}
