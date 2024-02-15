package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
import org.folio.dataexp.rest.resource.MappingProfilesApi;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class MappingProfileController implements MappingProfilesApi {

  private final FolioExecutionContext folioExecutionContext;
  private final MappingProfileEntityRepository mappingProfileEntityRepository;
  private final MappingProfileEntityCqlRepository mappingProfileEntityCqlRepository;
  private final UserClient userClient;

  @Override
  public ResponseEntity<Void> deleteMappingProfileById(UUID mappingProfileId) {
    var mappingProfileEntity = mappingProfileEntityRepository.getReferenceById(mappingProfileId);
    if (Boolean.TRUE.equals(mappingProfileEntity.getMappingProfile().getDefault()))
      throw new DefaultMappingProfileException("Deletion of default mapping profile is forbidden");
    mappingProfileEntityRepository.deleteById(mappingProfileId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<MappingProfile> getMappingProfileById(UUID mappingProfileId) {
    var mappingProfileEntity = mappingProfileEntityRepository.getReferenceById(mappingProfileId);
    return new ResponseEntity<>(mappingProfileEntity.getMappingProfile(), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<MappingProfileCollection> getMappingProfiles(String query, Integer offset, Integer limit) {
    if (StringUtils.isEmpty(query)) query = "(cql.allRecords=1)";
    var mappingProfilesPage  = mappingProfileEntityCqlRepository.findByCQL(query, OffsetRequest.of(offset, limit));
    var mappingProfiles = mappingProfilesPage.stream().map(MappingProfileEntity::getMappingProfile).toList();
    var mappingProfileCollection = new MappingProfileCollection();
    mappingProfileCollection.setMappingProfiles(mappingProfiles);
    mappingProfileCollection.setTotalRecords((int) mappingProfilesPage.getTotalElements());
    return new ResponseEntity<>(mappingProfileCollection, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<MappingProfile> postMappingProfile(MappingProfile mappingProfile) {
    var id = Objects.isNull(mappingProfile.getId()) ? UUID.randomUUID() : mappingProfile.getId();

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

    mappingProfile.setId(id);
    var mappingProfileEntity = MappingProfileEntity.builder()
      .id(mappingProfile.getId())
      .creationDate(LocalDateTime.now())
      .mappingProfile(mappingProfile)
      .name(mappingProfile.getName())
      .createdBy(folioExecutionContext.getUserId().toString()).build();
    var saved = mappingProfileEntityRepository.save(mappingProfileEntity);
    return new ResponseEntity<>(saved.getMappingProfile(), HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Void> putMappingProfile(UUID mappingProfileId, MappingProfile mappingProfile) {
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

    mappingProfileEntity.setMappingProfile(mappingProfile);
    mappingProfileEntityRepository.save(mappingProfileEntity);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
