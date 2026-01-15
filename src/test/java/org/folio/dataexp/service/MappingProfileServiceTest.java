package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.domain.dto.User;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.exception.mapping.profile.LockedMappingProfileException;
import org.folio.dataexp.repository.MappingProfileEntityCqlRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.service.validators.MappingProfileValidator;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

  @InjectMocks private MappingProfileService mappingProfileService;

  private UUID mappingProfileId;
  private MappingProfile mappingProfile;
  private MappingProfileEntity mappingProfileEntity;

  @BeforeEach
  void setUp() {
    mappingProfileId = UUID.randomUUID();
    mappingProfile = new MappingProfile();
    mappingProfile.setId(mappingProfileId);
    mappingProfile.setName("Test Profile");
    mappingProfileEntity = new MappingProfileEntity();
    mappingProfileEntity.setId(mappingProfileId);
    mappingProfileEntity.setMappingProfile(mappingProfile);
  }

  @Test
  void deleteMappingProfileByIdTest() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(false);
    mappingProfile.setName("mappingProfile");
    var entity =
        MappingProfileEntity.builder()
            .id(mappingProfile.getId())
            .mappingProfile(mappingProfile)
            .build();

    when(mappingProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);

    mappingProfileService.deleteMappingProfileById(mappingProfile.getId());

    verify(mappingProfileEntityRepository).deleteById(isA(UUID.class));
  }

  @Test
  @SneakyThrows
  void getMappingProfileByIdTest() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");

    var entity =
        MappingProfileEntity.builder()
            .id(mappingProfile.getId())
            .mappingProfile(mappingProfile)
            .build();
    when(mappingProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);

    mappingProfileService.getMappingProfileById(mappingProfile.getId());

    verify(mappingProfileEntityRepository).getReferenceById(isA(UUID.class));
  }

  @Test
  @SneakyThrows
  void getMappingProfiles() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");

    var entity =
        MappingProfileEntity.builder()
            .id(mappingProfile.getId())
            .mappingProfile(mappingProfile)
            .build();
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
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(true);
    mappingProfile.setName("mappingProfile");
    mappingProfile.setTransformations(List.of(transformation));
    var user = new User();
    user.setPersonal(new User.Personal());
    var entity =
        MappingProfileEntity.builder()
            .id(mappingProfile.getId())
            .mappingProfile(mappingProfile)
            .build();

    when(mappingProfileEntityRepository.save(isA(MappingProfileEntity.class))).thenReturn(entity);
    when(userClient.getUserById(isA(String.class))).thenReturn(user);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    mappingProfileService.postMappingProfile(mappingProfile);

    verify(mappingProfileEntityRepository).save(isA(MappingProfileEntity.class));
    verify(mappingProfileValidator).validate(isA(MappingProfile.class));
  }

  @Test
  void putMappingProfileTest() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.randomUUID());
    mappingProfile.setDefault(false);
    mappingProfile.setName("mappingProfile");
    mappingProfile.setMetadata(new Metadata().createdDate(new Date()));
    var user = new User();
    user.setPersonal(new User.Personal());

    var entity =
        MappingProfileEntity.builder()
            .id(mappingProfile.getId())
            .mappingProfile(mappingProfile)
            .build();
    when(mappingProfileEntityRepository.getReferenceById(isA(UUID.class))).thenReturn(entity);
    when(mappingProfileEntityRepository.save(isA(MappingProfileEntity.class))).thenReturn(entity);
    when(userClient.getUserById(isA(String.class))).thenReturn(user);
    when(folioExecutionContext.getUserId()).thenReturn(UUID.randomUUID());

    mappingProfileService.putMappingProfile(mappingProfile.getId(), mappingProfile);

    verify(mappingProfileEntityRepository).save(isA(MappingProfileEntity.class));
    verify(mappingProfileValidator).validate(isA(MappingProfile.class));
  }

  // Tests for lockProfile method

  @Test
  void shouldLockProfileSuccessfully_whenProfileIsNotLocked() {
    // Given
    UUID userId = UUID.randomUUID();
    mappingProfileEntity.setLocked(false);

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);

    // When
    mappingProfileService.lockMappingProfile(mappingProfileId);

    // Then
    verify(mappingProfileEntityRepository).getReferenceById(mappingProfileId);
    ArgumentCaptor<MappingProfileEntity> entityCaptor =
        ArgumentCaptor.forClass(MappingProfileEntity.class);
    verify(mappingProfileEntityRepository).save(entityCaptor.capture());

    MappingProfileEntity savedEntity = entityCaptor.getValue();
    assertThat(savedEntity.isLocked()).as("Profile should be locked").isTrue();
    assertThat(savedEntity.getLockedBy())
        .as("LockedBy should be set to current user")
        .isEqualTo(userId);
    assertThat(savedEntity.getLockedAt())
        .as("LockedAt should be set")
        .isNotNull()
        .isBeforeOrEqualTo(LocalDateTime.now());
  }

  @Test
  void shouldThrowLockedMappingProfileException_whenProfileIsAlreadyLocked() {
    // Given
    mappingProfileEntity.setLocked(true);
    mappingProfileEntity.setLockedAt(LocalDateTime.now().minusHours(1));
    mappingProfileEntity.setLockedBy(UUID.randomUUID());

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);

    // When & Then
    assertThatThrownBy(() -> mappingProfileService.lockMappingProfile(mappingProfileId))
        .isInstanceOf(LockedMappingProfileException.class)
        .hasMessage("Profile is already locked.");

    verify(mappingProfileEntityRepository).getReferenceById(mappingProfileId);
    verify(mappingProfileEntityRepository, never()).save(any());
    verify(folioExecutionContext, never()).getUserId();
  }

  @Test
  void shouldSetCorrectLockDetails_whenLockingProfile() {
    // Given
    UUID userId = UUID.randomUUID();
    LocalDateTime beforeLock = LocalDateTime.now();
    mappingProfileEntity.setLocked(false);
    mappingProfileEntity.setLockedBy(null);
    mappingProfileEntity.setLockedAt(null);

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);

    // When
    mappingProfileService.lockMappingProfile(mappingProfileId);

    // Then
    ArgumentCaptor<MappingProfileEntity> entityCaptor =
        ArgumentCaptor.forClass(MappingProfileEntity.class);
    verify(mappingProfileEntityRepository).save(entityCaptor.capture());

    MappingProfileEntity savedEntity = entityCaptor.getValue();
    LocalDateTime afterLock = LocalDateTime.now();

    assertThat(savedEntity.isLocked()).isTrue();
    assertThat(savedEntity.getLockedBy()).isEqualTo(userId);
    assertThat(savedEntity.getLockedAt()).isAfterOrEqualTo(beforeLock).isBeforeOrEqualTo(afterLock);
  }

  @Test
  void shouldLockNonDefaultProfile_successfully() {
    // Given
    UUID userId = UUID.randomUUID();
    mappingProfile.setDefault(false);
    mappingProfileEntity.setLocked(false);

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);

    // When
    mappingProfileService.lockMappingProfile(mappingProfileId);

    // Then
    verify(mappingProfileEntityRepository).save(any(MappingProfileEntity.class));
  }

  @Test
  void shouldLockDefaultProfile_successfully() {
    // Given
    UUID userId = UUID.randomUUID();
    mappingProfile.setDefault(true);
    mappingProfileEntity.setLocked(false);

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);
    when(folioExecutionContext.getUserId()).thenReturn(userId);

    // When
    mappingProfileService.lockMappingProfile(mappingProfileId);

    // Then
    verify(mappingProfileEntityRepository).save(any(MappingProfileEntity.class));
  }

  // Tests for unlockProfile method

  @Test
  void shouldUnlockProfileSuccessfully_whenProfileIsLockedAndNotDefault() {
    // Given
    mappingProfile.setDefault(false);
    mappingProfileEntity.setLocked(true);
    mappingProfileEntity.setLockedAt(LocalDateTime.now().minusHours(1));
    mappingProfileEntity.setLockedBy(UUID.randomUUID());

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);

    // When
    mappingProfileService.unlockMappingProfile(mappingProfileId);

    // Then
    ArgumentCaptor<MappingProfileEntity> entityCaptor =
        ArgumentCaptor.forClass(MappingProfileEntity.class);
    verify(mappingProfileEntityRepository).save(entityCaptor.capture());

    MappingProfileEntity savedEntity = entityCaptor.getValue();
    assertThat(savedEntity.isLocked()).isFalse();
    assertThat(savedEntity.getLockedBy()).isNull();
    assertThat(savedEntity.getLockedAt()).isNull();
  }

  @Test
  void shouldThrowLockedMappingProfileException_whenUnlockingAlreadyUnlockedProfile() {
    // Given
    mappingProfile.setDefault(false);
    mappingProfileEntity.setLocked(false);

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);

    // When & Then
    assertThatThrownBy(() -> mappingProfileService.unlockMappingProfile(mappingProfileId))
        .isInstanceOf(LockedMappingProfileException.class)
        .hasMessage("Profile is already unlocked.");

    verify(mappingProfileEntityRepository, never()).save(any());
  }

  @Test
  void shouldThrowLockedMappingProfileException_whenUnlockingDefaultProfile() {
    // Given
    mappingProfile.setDefault(true);
    mappingProfileEntity.setLocked(true);
    mappingProfileEntity.setLockedBy(UUID.randomUUID());

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);

    // When & Then
    assertThatThrownBy(() -> mappingProfileService.unlockMappingProfile(mappingProfileId))
        .isInstanceOf(LockedMappingProfileException.class)
        .hasMessage("Default mapping profile cannot be unlocked.");

    verify(mappingProfileEntityRepository, never()).save(any());
  }

  @Test
  void shouldClearAllLockDetails_whenUnlockingProfile() {
    // Given
    mappingProfile.setDefault(false);
    mappingProfileEntity.setLocked(true);
    mappingProfileEntity.setLockedBy(UUID.randomUUID());
    mappingProfileEntity.setLockedAt(LocalDateTime.now().minusHours(2));

    when(mappingProfileEntityRepository.getReferenceById(mappingProfileId))
        .thenReturn(mappingProfileEntity);

    // When
    mappingProfileService.unlockMappingProfile(mappingProfileId);

    // Then
    ArgumentCaptor<MappingProfileEntity> entityCaptor =
        ArgumentCaptor.forClass(MappingProfileEntity.class);
    verify(mappingProfileEntityRepository).save(entityCaptor.capture());

    MappingProfileEntity savedEntity = entityCaptor.getValue();
    assertThat(savedEntity.isLocked()).isFalse();
    assertThat(savedEntity.getLockedBy()).isNull();
    assertThat(savedEntity.getLockedAt()).isNull();
  }
}
