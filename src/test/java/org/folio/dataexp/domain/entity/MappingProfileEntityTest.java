package org.folio.dataexp.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.MappingProfile.OutputFormatEnum;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.UserInfo;
import org.junit.jupiter.api.Test;

class MappingProfileEntityTest {

  @Test
  @TestMate(name = "TestMate-d786d6d04e9a68c1acfa18e0a0562872")
  void testFromMappingProfileShouldGenerateIdWhenIdIsNull() {
    // Given
    var mappingProfile = new MappingProfile();
    // When
    var resultEntity = MappingProfileEntity.fromMappingProfile(mappingProfile);
    // Then
    assertThat(mappingProfile.getId()).isNotNull();
    assertThat(resultEntity.getId()).isEqualTo(mappingProfile.getId());
    assertThat(resultEntity.getMappingProfile()).isSameAs(mappingProfile);
  }

  @Test
  @TestMate(name = "TestMate-bf674c49cebce4dbdcea73a8790e807e")
  void testFromMappingProfileShouldUseExistingIdWhenIdIsNotNull() {
    // Given
    var existingId = UUID.fromString("c85d533c-a043-465c-a532-d62101086611");
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(existingId);
    // When
    var resultEntity = MappingProfileEntity.fromMappingProfile(mappingProfile);
    // Then
    assertThat(resultEntity.getId()).isEqualTo(existingId);
    assertThat(mappingProfile.getId()).isEqualTo(existingId);
    assertThat(resultEntity.getMappingProfile()).isSameAs(mappingProfile);
  }

  @Test
  @TestMate(name = "TestMate-26e2e4fecdcf755a45ddee2a668ff842")
  void testFromMappingProfileShouldMapAllFieldsFromFullyPopulatedDto() {
    // Given
    var createdByUserId = "user-created-id";
    var updatedByUserId = "user-updated-id";
    var createdDate = Instant.parse("2023-10-23T10:00:00Z");
    var updatedDate = Instant.parse("2023-10-23T11:30:00Z");
    var metadata = new Metadata();
    metadata.setCreatedByUserId(createdByUserId);
    metadata.setUpdatedByUserId(updatedByUserId);
    metadata.setCreatedDate(Date.from(createdDate));
    metadata.setUpdatedDate(Date.from(updatedDate));
    var userInfo = new UserInfo();
    userInfo.setFirstName("John");
    userInfo.setLastName("Doe");
    var profileId = UUID.fromString("c85d533c-a043-465c-a532-d62101086611");
    var lockedById = UUID.fromString("a043465c-c85d-4c53-a532-d62101086611");
    var lockedAt = Instant.parse("2023-10-23T12:00:00Z");
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(profileId);
    mappingProfile.setName("Fully Populated Profile");
    mappingProfile.setDescription("A detailed description.");
    mappingProfile.setRecordTypes(List.of(RecordTypes.SRS, RecordTypes.INSTANCE));
    mappingProfile.setOutputFormat(OutputFormatEnum.MARC);
    mappingProfile.setLocked(true);
    mappingProfile.setLockedBy(lockedById);
    mappingProfile.setLockedAt(Date.from(lockedAt));
    mappingProfile.setMetadata(metadata);
    mappingProfile.setUserInfo(userInfo);
    // When
    var resultEntity = MappingProfileEntity.fromMappingProfile(mappingProfile);
    // Then
    assertThat(resultEntity.getId()).isEqualTo(profileId);
    assertThat(resultEntity.getName()).isEqualTo("Fully Populated Profile");
    assertThat(resultEntity.getDescription()).isEqualTo("A detailed description.");
    assertThat(resultEntity.getMappingProfile()).isSameAs(mappingProfile);
    assertThat(resultEntity.getRecordTypes()).isEqualTo("SRS,INSTANCE");
    assertThat(resultEntity.getFormat()).isEqualTo("MARC");
    assertThat(resultEntity.getCreatedBy()).isEqualTo(createdByUserId);
    assertThat(resultEntity.getUpdatedByUserId()).isEqualTo(updatedByUserId);
    assertThat(resultEntity.getCreationDate()).isEqualTo(LocalDateTime.of(2023, 10, 23, 10, 0, 0));
    assertThat(resultEntity.getUpdatedDate()).isEqualTo(LocalDateTime.of(2023, 10, 23, 11, 30, 0));
    assertThat(resultEntity.getUpdatedByFirstName()).isEqualTo("John");
    assertThat(resultEntity.getUpdatedByLastName()).isEqualTo("Doe");
    assertThat(resultEntity.isLocked()).isTrue();
    assertThat(resultEntity.getLockedBy()).isEqualTo(lockedById);
    assertThat(resultEntity.getLockedAt()).isEqualTo(LocalDateTime.of(2023, 10, 23, 12, 0, 0));
  }

  @Test
  @TestMate(name = "TestMate-56adb873962fd79ced725f36e5f56745")
  void testFromMappingProfileShouldHandleNullAndEmptyNestedObjectsGracefully() {
    // Given
    var profileId = UUID.fromString("1f2a3b4c-5d6e-7f8a-9b0c-1d2e3f4a5b6c");
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(profileId);
    mappingProfile.setName("Profile with Nulls");
    mappingProfile.setLocked(false);
    mappingProfile.setMetadata(null);
    mappingProfile.setUserInfo(null);
    mappingProfile.setRecordTypes(null);
    mappingProfile.setOutputFormat(null);
    mappingProfile.setLockedAt(null);
    // When
    var resultEntity = MappingProfileEntity.fromMappingProfile(mappingProfile);
    // Then
    assertThat(resultEntity.getId()).isEqualTo(profileId);
    assertThat(resultEntity.getName()).isEqualTo("Profile with Nulls");
    assertThat(resultEntity.isLocked()).isFalse();
    assertThat(resultEntity.getMappingProfile()).isSameAs(mappingProfile);
    assertThat(resultEntity.getCreationDate()).isNull();
    assertThat(resultEntity.getCreatedBy()).isNull();
    assertThat(resultEntity.getUpdatedDate()).isNull();
    assertThat(resultEntity.getUpdatedByUserId()).isNull();
    assertThat(resultEntity.getUpdatedByFirstName()).isNull();
    assertThat(resultEntity.getUpdatedByLastName()).isNull();
    assertThat(resultEntity.getRecordTypes()).isNull();
    assertThat(resultEntity.getFormat()).isNull();
    assertThat(resultEntity.getLockedAt()).isNull();
  }

  @Test
  @TestMate(name = "TestMate-d3138899bc89c25a434e245ef25e8a9a")
  void testFromMappingProfileShouldHandlePartiallyPopulatedNestedObjects() {
    // Given
    var createdDate = Instant.parse("2023-01-01T10:00:00Z");
    var metadata = new Metadata();
    metadata.setCreatedByUserId("creator-id");
    metadata.setCreatedDate(Date.from(createdDate));
    metadata.setUpdatedDate(null);
    metadata.setUpdatedByUserId(null);
    var userInfo = new UserInfo();
    userInfo.setFirstName("John");
    userInfo.setLastName(null);
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(UUID.fromString("a1b2c3d4-e5f6-a7b8-c9d0-e1f2a3b4c5d6"));
    mappingProfile.setMetadata(metadata);
    mappingProfile.setUserInfo(userInfo);
    // When
    var resultEntity = MappingProfileEntity.fromMappingProfile(mappingProfile);
    // Then
    assertThat(resultEntity.getCreationDate()).isEqualTo(LocalDateTime.of(2023, 1, 1, 10, 0, 0));
    assertThat(resultEntity.getCreatedBy()).isEqualTo("creator-id");
    assertThat(resultEntity.getUpdatedByFirstName()).isEqualTo("John");
    assertThat(resultEntity.getUpdatedDate()).isNull();
    assertThat(resultEntity.getUpdatedByUserId()).isNull();
    assertThat(resultEntity.getUpdatedByLastName()).isNull();
    assertThat(resultEntity.getMappingProfile()).isSameAs(mappingProfile);
  }
}
