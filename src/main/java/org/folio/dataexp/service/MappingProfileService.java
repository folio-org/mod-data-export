package org.folio.dataexp.service;

import static java.lang.Boolean.TRUE;

import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.MappingProfileCollection;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.UserInfo;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.exception.mapping.profile.DefaultMappingProfileException;
import org.folio.dataexp.exception.mapping.profile.LockMappingProfilePermissionException;
import org.folio.dataexp.repository.MappingProfileEntityCqlRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.service.validators.MappingProfileValidator;
import org.folio.dataexp.service.validators.PermissionsValidator;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;

/** Service for managing mapping profiles. */
@Service
@RequiredArgsConstructor
public class MappingProfileService {

  private final FolioExecutionContext folioExecutionContext;
  private final MappingProfileEntityRepository mappingProfileEntityRepository;
  private final MappingProfileEntityCqlRepository mappingProfileEntityCqlRepository;
  private final UserClient userClient;
  private final MappingProfileValidator mappingProfileValidator;
  private final PermissionsValidator permissionsValidator;

  /**
   * Deletes a mapping profile by its ID.
   *
   * @param mappingProfileId The mapping profile UUID.
   * @throws DefaultMappingProfileException if the profile is default.
   */
  public void deleteMappingProfileById(UUID mappingProfileId) {
    var mappingProfileEntity = mappingProfileEntityRepository.getReferenceById(mappingProfileId);
    if (TRUE.equals(mappingProfileEntity.getMappingProfile().getDefault())) {
      throw new DefaultMappingProfileException("Deletion of default mapping profile is forbidden");
    }
    mappingProfileEntityRepository.deleteById(mappingProfileId);
  }

  /**
   * Retrieves a mapping profile entity by its ID.
   *
   * @param mappingProfileId The mapping profile UUID.
   * @return The MappingProfileEntity.
   */
  public MappingProfileEntity getMappingProfileById(UUID mappingProfileId) {
    return mappingProfileEntityRepository.getReferenceById(mappingProfileId);
  }

  /**
   * Retrieves mapping profiles by CQL query, offset, and limit.
   *
   * @param query The CQL query.
   * @param offset The offset.
   * @param limit The limit.
   * @return MappingProfileCollection containing results.
   */
  public MappingProfileCollection getMappingProfiles(String query, Integer offset, Integer limit) {
    if (StringUtils.isEmpty(query)) {
      query = "(cql.allRecords=1)";
    }
    var mappingProfilesPage =
        mappingProfileEntityCqlRepository.findByCql(query, OffsetRequest.of(offset, limit));
    var mappingProfiles =
        mappingProfilesPage.stream().map(MappingProfileEntity::getMappingProfile).toList();
    var mappingProfileCollection = new MappingProfileCollection();
    mappingProfileCollection.setMappingProfiles(mappingProfiles);
    mappingProfileCollection.setTotalRecords((int) mappingProfilesPage.getTotalElements());
    return mappingProfileCollection;
  }

  /**
   * Creates and saves a new mapping profile.
   *
   * @param mappingProfile The MappingProfile to save.
   * @return The saved MappingProfile.
   */
  public MappingProfile postMappingProfile(MappingProfile mappingProfile) {
    var userId = folioExecutionContext.getUserId().toString();
    var user = userClient.getUserById(userId);
    var userInfo = new UserInfo();
    userInfo.setFirstName(user.getPersonal().getFirstName());
    userInfo.setLastName(user.getPersonal().getLastName());
    userInfo.setUserName(user.getUsername());
    mappingProfile.setUserInfo(userInfo);

    var metaData = new Metadata();
    metaData.createdByUserId(userId);
    metaData.updatedByUserId(userId);
    var current = new Date();
    metaData.createdDate(current);
    metaData.updatedDate(current);

    metaData.createdByUsername(user.getUsername());
    metaData.updatedByUsername(user.getUsername());
    mappingProfile.setMetadata(metaData);

    if (mappingProfile.getLocked()) {
      lockProfile(mappingProfile);
    }

    mappingProfileValidator.validate(mappingProfile);

    var saved =
        mappingProfileEntityRepository.save(
            MappingProfileEntity.fromMappingProfile(mappingProfile));
    return saved.getMappingProfile();
  }

  /**
   * Updates an existing mapping profile.
   *
   * @param mappingProfileId The mapping profile UUID.
   * @param mappingProfile The MappingProfile to update.
   * @throws DefaultMappingProfileException if the profile is default.
   */
  public void putMappingProfile(UUID mappingProfileId, MappingProfile mappingProfile) {
    var mappingProfileEntity = mappingProfileEntityRepository.getReferenceById(mappingProfileId);
    if (TRUE.equals(mappingProfileEntity.getMappingProfile().getDefault())) {
      throw new DefaultMappingProfileException("Editing of default mapping profile is forbidden");
    }

    var userId = folioExecutionContext.getUserId().toString();
    var user = userClient.getUserById(userId);

    var userInfo = new UserInfo();
    userInfo.setFirstName(user.getPersonal().getFirstName());
    userInfo.setLastName(user.getPersonal().getLastName());
    userInfo.setUserName(user.getUsername());
    mappingProfile.setUserInfo(userInfo);

    var metadataOfExistingMappingProfile = mappingProfileEntity.getMappingProfile().getMetadata();

    var metadata =
        Metadata.builder()
            .createdDate(metadataOfExistingMappingProfile.getCreatedDate())
            .updatedDate(new Date())
            .createdByUserId(metadataOfExistingMappingProfile.getCreatedByUserId())
            .updatedByUserId(userId)
            .createdByUsername(metadataOfExistingMappingProfile.getCreatedByUsername())
            .updatedByUsername(user.getUsername())
            .build();

    mappingProfile.setMetadata(metadata);
    updateLock(mappingProfileEntity, mappingProfile);
    mappingProfileValidator.validate(mappingProfile);

    mappingProfileEntityRepository.save(MappingProfileEntity.fromMappingProfile(mappingProfile));
  }

  private void updateLock(
      MappingProfileEntity mappingProfileEntity, MappingProfile mappingProfile) {
    boolean existingLockStatus = mappingProfileEntity.isLocked();
    boolean newLockStatus = Boolean.TRUE.equals(mappingProfile.getLocked());
    if (existingLockStatus != newLockStatus) {
      if (newLockStatus) {
        lockProfile(mappingProfile);
      } else {
        unlockProfile(mappingProfile);
      }
    }
  }

  private void lockProfile(MappingProfile mappingProfile) {
    if (permissionsValidator.checkLockMappingProfilePermission()) {
      mappingProfile.setLocked(true);
      mappingProfile.setLockedAt(new Date());
      mappingProfile.setLockedBy(folioExecutionContext.getUserId());
    } else {
      throw new LockMappingProfilePermissionException(
          "You do not have permission to lock this profile.");
    }
  }

  private void unlockProfile(MappingProfile mappingProfile) {
    if (permissionsValidator.checkLockMappingProfilePermission()) {
      mappingProfile.setLocked(false);
      mappingProfile.setLockedAt(null);
      mappingProfile.setLockedBy(null);
    } else {
      throw new LockMappingProfilePermissionException(
          "You do not have permission to unlock this profile.");
    }
  }
}
