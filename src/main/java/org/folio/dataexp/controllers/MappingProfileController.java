package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.MappingProfileCollection;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.exception.job.profile.DefaultJobProfileException;
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
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class MappingProfileController implements MappingProfilesApi {

  private final FolioExecutionContext folioExecutionContext;
  private final MappingProfileEntityRepository mappingProfileEntityRepository;
  private final MappingProfileEntityCqlRepository mappingProfileEntityCqlRepository;

  @Override
  public ResponseEntity<Void> deleteMappingProfileById(UUID mappingProfileId) {
    var mappingProfileEntity = mappingProfileEntityRepository.getReferenceById(mappingProfileId);
    if (mappingProfileEntity.getMappingProfile().getDefault())
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
    var mappingProfiles  = mappingProfileEntityCqlRepository.findByCQL(query, OffsetRequest.of(offset, limit))
      .map(MappingProfileEntity::getMappingProfile).stream().toList();
    var mappingProfileCollection = new MappingProfileCollection();
    mappingProfileCollection.setMappingProfiles(mappingProfiles);
    mappingProfileCollection.setTotalRecords(mappingProfiles.size());
    return new ResponseEntity<>(mappingProfileCollection, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<MappingProfile> postMappingProfile(MappingProfile mappingProfile) {
    var mappingProfileEntity = MappingProfileEntity.builder()
      .id(mappingProfile.getId())
      .creationDate(LocalDateTime.now())
      .mappingProfile(mappingProfile)
      .createdBy(folioExecutionContext.getUserId().toString()).build();
    var saved = mappingProfileEntityRepository.save(mappingProfileEntity);
    return new ResponseEntity<>(saved.getMappingProfile(), HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Void> putMappingProfile(UUID mappingProfileId, MappingProfile mappingProfile) {
    var mappingProfileEntity = mappingProfileEntityRepository.getReferenceById(mappingProfileId);
    if (mappingProfileEntity.getMappingProfile().getDefault())
      throw new DefaultMappingProfileException("Editing of default mapping profile is forbidden");
    mappingProfileEntity.setMappingProfile(mappingProfile);
    mappingProfileEntityRepository.save(mappingProfileEntity);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
