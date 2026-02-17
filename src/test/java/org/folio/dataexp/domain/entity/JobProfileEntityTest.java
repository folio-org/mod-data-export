package org.folio.dataexp.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.UserInfo;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class JobProfileEntityTest {

  @Test
  @TestMate(name = "TestMate-ed5337479f1b934ef8049572ef2f8704")
  void testFromJobProfileShouldMapAllFieldsWhenJobProfileIsFullyPopulated() {
    // Given
    var createdByUserId = "551dcf36-cd44-42f4-bace-f2770d5e0a25";
    var updatedByUserId = "9a1309cb-75be-4d7d-8a86-e0dccc4428bb";
    var createdDate = Instant.parse("2023-01-10T10:00:00Z");
    var updatedDate = Instant.parse("2023-01-11T11:30:00Z");

    var metadata = new Metadata();
    metadata.setCreatedDate(Date.from(createdDate));
    metadata.setCreatedByUserId(createdByUserId);
    metadata.setUpdatedDate(Date.from(updatedDate));
    metadata.setUpdatedByUserId(updatedByUserId);
    metadata.setCreatedByUsername("creator");
    metadata.setUpdatedByUsername("updater");

    var userInfo = new UserInfo();
    userInfo.setFirstName("John");
    userInfo.setLastName("Doe");
    userInfo.setUserName("johndoe");

    var jobProfileId = UUID.fromString("c832a513-c672-43d7-83f1-35b4ad193913");
    var mappingProfileId = UUID.fromString("a832a513-c672-43d7-83f1-35b4ad19391a");
    var lockedById = UUID.fromString("b832a513-c672-43d7-83f1-35b4ad19391b");
    var lockedAt = Instant.parse("2023-01-12T12:00:00Z");
    var jobProfile = new JobProfile();
    jobProfile.setId(jobProfileId);
    jobProfile.setName("Test Profile");
    jobProfile.setDescription("Test Description");
    jobProfile.setMappingProfileId(mappingProfileId);
    jobProfile.setLocked(true);
    jobProfile.setLockedBy(lockedById);
    jobProfile.setLockedAt(Date.from(lockedAt));
    jobProfile.setUserInfo(userInfo);
    jobProfile.setMetadata(metadata);

    var expectedCreationDateTime = LocalDateTime.of(2023, 1, 10, 10, 0, 0);
    var expectedUpdateDateTime = LocalDateTime.of(2023, 1, 11, 11, 30, 0);
    var expectedLockDateTime = LocalDateTime.of(2023, 1, 12, 12, 0, 0);

    // When
    var actualEntity = JobProfileEntity.fromJobProfile(jobProfile);
    // Then
    assertThat(actualEntity.getId()).isEqualTo(jobProfileId);
    assertThat(actualEntity.getName()).isEqualTo("Test Profile");
    assertThat(actualEntity.getDescription()).isEqualTo("Test Description");
    assertThat(actualEntity.getMappingProfileId()).isEqualTo(mappingProfileId);
    assertThat(actualEntity.getJobProfile()).isSameAs(jobProfile);
    assertThat(actualEntity.getCreatedBy()).isEqualTo(createdByUserId);
    assertThat(actualEntity.getUpdatedByUserId()).isEqualTo(updatedByUserId);
    assertThat(actualEntity.getCreationDate()).isEqualTo(expectedCreationDateTime);
    assertThat(actualEntity.getUpdatedDate()).isEqualTo(expectedUpdateDateTime);
    assertThat(actualEntity.getUpdatedByFirstName()).isEqualTo("John");
    assertThat(actualEntity.getUpdatedByLastName()).isEqualTo("Doe");
    assertThat(actualEntity.isLocked()).isTrue();
    assertThat(actualEntity.getLockedBy()).isEqualTo(lockedById);
    assertThat(actualEntity.getLockedAt()).isEqualTo(expectedLockDateTime);
  }

  @Test
  @TestMate(name = "TestMate-f4b75bd87f4a27224e8bfe31c60daa9c")
  void testFromJobProfileShouldGenerateIdWhenJobProfileIdIsNull() {
    // Given
    var expectedGeneratedId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    var jobProfile = new JobProfile();
    jobProfile.setId(null);
    jobProfile.setName("Test Profile With Null ID");
    try (MockedStatic<UUID> mockedUuid = Mockito.mockStatic(UUID.class)) {
      mockedUuid.when(UUID::randomUUID).thenReturn(expectedGeneratedId);
      // When
      var actualEntity = JobProfileEntity.fromJobProfile(jobProfile);
      // Then
      assertThat(actualEntity.getId()).isEqualTo(expectedGeneratedId);
      assertThat(jobProfile.getId()).isEqualTo(expectedGeneratedId);
      assertThat(actualEntity.getName()).isEqualTo("Test Profile With Null ID");
    }
  }

  @Test
  @TestMate(name = "TestMate-53b9943e57069fcc084cbbfe7ae8f30e")
  void testFromJobProfileShouldHandleNullMetadataAndUserInfo() {
    // Given
    var jobProfileId = UUID.fromString("d3a3a3a3-c672-43d7-83f1-35b4ad193913");
    var mappingProfileId = UUID.fromString("b3a3a3a3-c672-43d7-83f1-35b4ad19391a");
    var jobProfile = new JobProfile();
    jobProfile.setId(jobProfileId);
    jobProfile.setName("Test Profile with Nulls");
    jobProfile.setMappingProfileId(mappingProfileId);
    jobProfile.setMetadata(null);
    jobProfile.setUserInfo(null);
    // When
    var actualEntity = JobProfileEntity.fromJobProfile(jobProfile);
    // Then
    assertThat(actualEntity.getId()).isEqualTo(jobProfileId);
    assertThat(actualEntity.getName()).isEqualTo("Test Profile with Nulls");
    assertThat(actualEntity.getJobProfile()).isSameAs(jobProfile);
    assertThat(actualEntity.getMappingProfileId()).isEqualTo(mappingProfileId);
    assertThat(actualEntity.getCreationDate()).isNull();
    assertThat(actualEntity.getCreatedBy()).isNull();
    assertThat(actualEntity.getUpdatedDate()).isNull();
    assertThat(actualEntity.getUpdatedByUserId()).isNull();
    assertThat(actualEntity.getUpdatedByFirstName()).isNull();
    assertThat(actualEntity.getUpdatedByLastName()).isNull();
  }
}
