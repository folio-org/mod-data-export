package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.JobProfileCollection;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.UserInfo;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.exception.job.profile.DefaultJobProfileException;
import org.folio.dataexp.repository.JobProfileEntityCqlRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.rest.resource.JobProfilesApi;
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
public class JobProfileController implements JobProfilesApi {

  private final FolioExecutionContext folioExecutionContext;
  private final JobProfileEntityRepository jobProfileEntityRepository;
  private final JobProfileEntityCqlRepository jobProfileEntityCqlRepository;
  private final UserClient userClient;

  @Override
  public ResponseEntity<Void> deleteJobProfileById(UUID jobProfileId) {
    var jobProfileEntity = jobProfileEntityRepository.getReferenceById(jobProfileId);
    if (Boolean.TRUE.equals(jobProfileEntity.getJobProfile().getDefault()))
      throw new DefaultJobProfileException("Deletion of default job profile is forbidden");
    jobProfileEntityRepository.deleteById(jobProfileId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<JobProfile> getJobProfileById(UUID jobProfileId) {
    var jobProfileEntity = jobProfileEntityRepository.getReferenceById(jobProfileId);
    return new ResponseEntity<>(jobProfileEntity.getJobProfile(), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<JobProfileCollection> getJobProfiles(String query, Integer offset, Integer limit) {
    if (StringUtils.isEmpty(query)) query = "(cql.allRecords=1)";
    var jobProfilesPage  = jobProfileEntityCqlRepository.findByCQL(query, OffsetRequest.of(offset, limit));
    var jobProfiles =  jobProfilesPage.toList().stream().map(JobProfileEntity::getJobProfile).toList();
    var jobProfileCollection = new JobProfileCollection();
    jobProfileCollection.setJobProfiles(jobProfiles);
    jobProfileCollection.setTotalRecords((int) jobProfilesPage.getTotalElements());
    return new ResponseEntity<>(jobProfileCollection, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<JobProfile> postJobProfile(JobProfile jobProfile) {
    var id = Objects.isNull(jobProfile.getId()) ? UUID.randomUUID() : jobProfile.getId();

    var userId = folioExecutionContext.getUserId().toString();
    var user = userClient.getUserById(userId);

    var userInfo = new UserInfo();
    userInfo.setFirstName(user.getPersonal().getFirstName());
    userInfo.setLastName(user.getPersonal().getLastName());
    userInfo.setUserName(user.getUsername());
    jobProfile.setUserInfo(userInfo);

    var metaData = new Metadata();
    metaData.createdByUserId(userId);
    metaData.updatedByUserId(userId);
    var current = new Date();
    metaData.createdDate(current);
    metaData.updatedDate(current);
    metaData.createdByUsername(user.getUsername());
    metaData.updatedByUsername(user.getUsername());
    jobProfile.setMetadata(metaData);

    jobProfile.setId(id);
    var jobProfileEntity = JobProfileEntity.builder()
      .id(jobProfile.getId())
      .creationDate(LocalDateTime.now())
      .jobProfile(jobProfile)
      .mappingProfileId(jobProfile.getMappingProfileId())
      .name(jobProfile.getName())
      .createdBy(folioExecutionContext.getUserId().toString()).build();
    var saved = jobProfileEntityRepository.save(jobProfileEntity);
    return new ResponseEntity<>(saved.getJobProfile(), HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Void> putJobProfile(UUID jobProfileId, JobProfile jobProfile) {
    var jobProfileEntity = jobProfileEntityRepository.getReferenceById(jobProfileId);
    if (Boolean.TRUE.equals(jobProfileEntity.getJobProfile().getDefault()))
      throw new DefaultJobProfileException("Editing of default job profile is forbidden");
    jobProfileEntity.setJobProfile(jobProfile);
    jobProfileEntity.setName(jobProfile.getName());
    jobProfileEntity.setMappingProfileId(jobProfile.getMappingProfileId());

    var userId = folioExecutionContext.getUserId().toString();
    var user = userClient.getUserById(userId);

    var userInfo = new UserInfo();
    userInfo.setFirstName(user.getPersonal().getFirstName());
    userInfo.setLastName(user.getPersonal().getLastName());
    userInfo.setUserName(user.getUsername());
    jobProfile.setUserInfo(userInfo);

    var metadata = jobProfile.getMetadata();
    metadata.updatedDate(new Date());
    metadata.updatedByUserId(userId);
    metadata.updatedByUsername(user.getUsername());

    jobProfileEntityRepository.save(jobProfileEntity);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
