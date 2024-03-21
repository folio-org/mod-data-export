package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.MappingProfileCollection;
import org.folio.dataexp.rest.resource.MappingProfilesApi;
import org.folio.dataexp.service.MappingProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class MappingProfileController implements MappingProfilesApi {

  private final MappingProfileService mappingProfileService;

  @Override
  public ResponseEntity<Void> deleteMappingProfileById(UUID mappingProfileId) {
    mappingProfileService.deleteMappingProfileById(mappingProfileId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<MappingProfile> getMappingProfileById(UUID mappingProfileId) {
    var mappingProfileEntity = mappingProfileService.getMappingProfileById(mappingProfileId);
    return new ResponseEntity<>(mappingProfileEntity.getMappingProfile(), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<MappingProfileCollection> getMappingProfiles(String query, Integer offset, Integer limit) {
    var mappingProfileCollection = mappingProfileService.getMappingProfiles(query, offset, limit);
    return new ResponseEntity<>(mappingProfileCollection, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<MappingProfile> postMappingProfile(MappingProfile mappingProfile) {
    var saved = mappingProfileService.postMappingProfile(mappingProfile);
    return new ResponseEntity<>(saved, HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Void> putMappingProfile(UUID mappingProfileId, MappingProfile mappingProfile) {
    mappingProfileService.putMappingProfile(mappingProfileId, mappingProfile);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
