package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.MappingProfileCollection;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.UserInfo;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.exception.mapping.profile.DefaultMappingProfileException;
import org.folio.dataexp.repository.MappingProfileEntityCqlRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.service.validators.MappingProfileValidator;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MappingProfileService {

  private final FolioExecutionContext folioExecutionContext;
  private final MappingProfileEntityRepository mappingProfileEntityRepository;
  private final MappingProfileEntityCqlRepository mappingProfileEntityCqlRepository;
  private final UserClient userClient;
  private final MappingProfileValidator mappingProfileValidator;

  public void deleteMappingProfileById(UUID mappingProfileId) {
    var mappingProfileEntity = mappingProfileEntityRepository.getReferenceById(mappingProfileId);
    if (Boolean.TRUE.equals(mappingProfileEntity.getMappingProfile().getDefault()))
      throw new DefaultMappingProfileException("Deletion of default mapping profile is forbidden");
    mappingProfileEntityRepository.deleteById(mappingProfileId);
  }

  public MappingProfileEntity getMappingProfileById(UUID mappingProfileId) {
    return mappingProfileEntityRepository.getReferenceById(mappingProfileId);
  }

  public MappingProfileCollection getMappingProfiles(String query, Integer offset, Integer limit) {
    if (StringUtils.isEmpty(query)) query = "(cql.allRecords=1)";
    var mappingProfilesPage = mappingProfileEntityCqlRepository.findByCql(query, OffsetRequest.of(offset, limit));
    var mappingProfiles = mappingProfilesPage.stream().map(MappingProfileEntity::getMappingProfile).toList();
    var mappingProfileCollection = new MappingProfileCollection();
    mappingProfileCollection.setMappingProfiles(mappingProfiles);
    mappingProfileCollection.setTotalRecords((int) mappingProfilesPage.getTotalElements());
    return mappingProfileCollection;
  }

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

    mappingProfileValidator.validate(mappingProfile);

    var saved = mappingProfileEntityRepository.save(MappingProfileEntity.fromMappingProfile(mappingProfile));
    return saved.getMappingProfile();
  }

  public void putMappingProfile(UUID mappingProfileId, MappingProfile mappingProfile) {
    var mappingProfileEntity = mappingProfileEntityRepository.getReferenceById(mappingProfileId);
    if (Boolean.TRUE.equals(mappingProfileEntity.getMappingProfile().getDefault())) {
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

    var metadata = Metadata.builder()
      .createdDate(metadataOfExistingMappingProfile.getCreatedDate())
      .updatedDate(new Date())
      .createdByUserId(metadataOfExistingMappingProfile.getCreatedByUserId())
      .updatedByUserId(userId)
      .createdByUsername(metadataOfExistingMappingProfile.getCreatedByUsername())
      .updatedByUsername(user.getUsername())
      .build();

    mappingProfile.setMetadata(metadata);

    mappingProfileValidator.validate(mappingProfile);

    mappingProfileEntityRepository.save(MappingProfileEntity.fromMappingProfile(mappingProfile));
  }

}
