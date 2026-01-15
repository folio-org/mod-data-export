package org.folio.dataexp.service;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static org.folio.dataexp.util.S3FilePathUtils.getPathToStoredFiles;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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
import org.folio.dataexp.exception.job.profile.LockedJobProfileException;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.repository.JobProfileEntityCqlRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.data.OffsetRequest;
import org.springframework.stereotype.Service;

/** Service for managing job profiles, including deletion and associated file handling. */
@Service
@RequiredArgsConstructor
@Log4j2
public class JobProfileService {

  private final JobProfileEntityRepository jobProfileEntityRepository;
  private final FolioS3Client s3Client;
  private final UserClient userClient;
  private final JobExecutionService jobExecutionService;
  private final ErrorLogEntityCqlRepository errorLogEntityCqlRepository;
  private final JobProfileEntityCqlRepository jobProfileEntityCqlRepository;
  private final FolioExecutionContext folioExecutionContext;

  /**
   * Retrieves a job profile by its ID.
   *
   * @param jobProfileId the UUID of the job profile to retrieve
   * @return the JobProfile object
   */
  public JobProfile getJobProfileById(UUID jobProfileId) {
    return jobProfileEntityRepository.getReferenceById(jobProfileId).getJobProfile();
  }

  /**
   * Retrieves job profiles based on usage or query.
   *
   * @param used whether to filter by used profiles
   * @param query CQL query string
   * @param offset offset for pagination
   * @param limit limit for pagination
   * @return job profile collection
   */
  public JobProfileCollection getJobProfiles(
      Boolean used, String query, Integer offset, Integer limit) {
    if (TRUE.equals(used)) {
      return getUsedJobProfiles(offset, limit);
    }
    return getListOfJobProfiles(query, offset, limit);
  }

  /**
   * Creates a new job profile.
   *
   * @param jobProfile the JobProfile object to create
   * @return the created JobProfile object
   */
  public JobProfile postJobProfile(JobProfile jobProfile) {
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

    var saved = jobProfileEntityRepository.save(JobProfileEntity.fromJobProfile(jobProfile));
    return saved.getJobProfile();
  }

  /**
   * Updates a job profile by its ID.
   *
   * @param jobProfileId the UUID of the job profile to update
   * @param jobProfile the JobProfile object with updated data
   * @throws DefaultJobProfileException if the job profile is marked as default
   */
  public void putJobProfile(UUID jobProfileId, JobProfile jobProfile) {
    var jobProfileEntity = jobProfileEntityRepository.getReferenceById(jobProfileId);
    if (TRUE.equals(jobProfileEntity.getJobProfile().getDefault())) {
      throw new DefaultJobProfileException("Editing of default job profile is forbidden");
    }

    var userId = folioExecutionContext.getUserId().toString();
    var user = userClient.getUserById(userId);

    var userInfo = new UserInfo();
    userInfo.setFirstName(user.getPersonal().getFirstName());
    userInfo.setLastName(user.getPersonal().getLastName());
    userInfo.setUserName(user.getUsername());
    jobProfile.setUserInfo(userInfo);

    var metadataOfExistingJobProfile = jobProfileEntity.getJobProfile().getMetadata();

    var metadata =
        Metadata.builder()
            .createdDate(metadataOfExistingJobProfile.getCreatedDate())
            .updatedDate(new Date())
            .createdByUserId(metadataOfExistingJobProfile.getCreatedByUserId())
            .updatedByUserId(userId)
            .createdByUsername(metadataOfExistingJobProfile.getCreatedByUsername())
            .updatedByUsername(user.getUsername())
            .build();

    jobProfile.setMetadata(metadata);
    jobProfileEntityRepository.save(JobProfileEntity.fromJobProfile(jobProfile));
  }

  /**
   * Deletes a job profile by its ID after checking if it is not a default or locked profile. Also
   * deletes associated exported files and disables their links.
   *
   * @param jobProfileId the UUID of the job profile to be deleted
   * @throws DefaultJobProfileException if the job profile is marked as default
   * @throws LockedJobProfileException if the job profile is locked
   */
  public void deleteJobProfileById(UUID jobProfileId) {
    var jobProfileEntity = jobProfileEntityRepository.getReferenceById(jobProfileId);
    if (TRUE.equals(jobProfileEntity.getJobProfile().getDefault())) {
      throw new DefaultJobProfileException("Deletion of default job profile is forbidden");
    }
    if (!jobProfileEntity.isLocked()) {
      deleteAssociatedErrors(jobProfileId);
      deleteExportedFilesAndDisableLink(jobProfileId);
      jobProfileEntityRepository.deleteById(jobProfileId);
    } else {
      throw new LockedJobProfileException(
          "This profile is locked. Please unlock the profile to proceed with editing/deletion.");
    }
  }

  /**
   * Checks if a job profile exists by its ID.
   *
   * @param jobProfileId the UUID of the job profile to check
   * @return true if the job profile exists, false otherwise
   */
  public boolean jobProfileExists(UUID jobProfileId) {
    if (isNull(jobProfileId)) {
      return false;
    }
    return jobProfileEntityRepository.existsById(jobProfileId);
  }

  /**
   * Locks a job profile by its ID.
   *
   * @param jobProfileId the UUID of the job profile to lock
   * @throws LockedJobProfileException if the job profile is already locked
   */
  public void lockProfile(UUID jobProfileId) {
    var jobProfileEntity = jobProfileEntityRepository.getReferenceById(jobProfileId);
    if (jobProfileEntity.isLocked()) {
      throw new LockedJobProfileException("Profile is already locked.");
    }
    jobProfileEntity.setLocked(true);
    jobProfileEntity.setLockedAt(LocalDateTime.now());
    jobProfileEntity.setLockedBy(folioExecutionContext.getUserId());
    jobProfileEntityRepository.save(jobProfileEntity);
  }

  /**
   * Unlocks a job profile by its ID.
   *
   * @param jobProfileId the UUID of the job profile to unlock
   * @throws LockedJobProfileException if the job profile is already unlocked or is a default
   *     profile
   */
  public void unlockProfile(UUID jobProfileId) {
    var jobProfileEntity = jobProfileEntityRepository.getReferenceById(jobProfileId);
    if (!jobProfileEntity.isLocked()) {
      throw new LockedJobProfileException("Profile is already unlocked.");
    }
    if (TRUE.equals(jobProfileEntity.getJobProfile().getDefault())) {
      throw new LockedJobProfileException("Default job profile cannot be unlocked.");
    }
    jobProfileEntity.setLocked(false);
    jobProfileEntity.setLockedAt(null);
    jobProfileEntity.setLockedBy(null);
    jobProfileEntityRepository.save(jobProfileEntity);
  }

  /**
   * Gets a collection of used job profiles.
   *
   * @param offset offset for pagination
   * @param limit limit for pagination
   * @return job profile collection
   */
  private JobProfileCollection getUsedJobProfiles(Integer offset, Integer limit) {
    log.info("getUsedJobProfiles::");

    List<Object[]> jobProfileData =
        jobProfileEntityCqlRepository.getUsedJobProfilesData(offset, limit);

    var jobProfiles =
        jobProfileData.stream()
            .filter(i -> Objects.nonNull(i[0]) && Objects.nonNull(i[1]))
            .map(i -> JobProfile.builder().id((UUID) i[0]).name((String) i[1]).build())
            .toList();

    var jobProfileCollection = new JobProfileCollection();
    jobProfileCollection.setJobProfiles(jobProfiles);
    jobProfileCollection.setTotalRecords(jobProfileData.size());
    return jobProfileCollection;
  }

  /**
   * Gets a list of job profiles by query.
   *
   * @param query CQL query string
   * @param offset offset for pagination
   * @param limit limit for pagination
   * @return job profile collection
   */
  private JobProfileCollection getListOfJobProfiles(String query, Integer offset, Integer limit) {
    log.info("getListOfJobProfiles::");
    if (StringUtils.isEmpty(query)) {
      query = "(cql.allRecords=1)";
    }
    var jobProfilesPage =
        jobProfileEntityCqlRepository.findByCql(query, OffsetRequest.of(offset, limit));
    var jobProfiles = jobProfilesPage.stream().map(JobProfileEntity::getJobProfile).toList();
    var jobProfileCollection = new JobProfileCollection();
    jobProfileCollection.setJobProfiles(jobProfiles);
    jobProfileCollection.setTotalRecords((int) jobProfilesPage.getTotalElements());
    return jobProfileCollection;
  }

  /**
   * Deletes exported files associated with job executions linked to the specified job profile ID
   * from S3 and disables the links to those files. Also, it removes the job profile ID reference
   * from the job executions.
   *
   * @param jobProfileId the UUID of the job profile whose associated exported files are to be
   *     deleted and links disabled
   */
  private void deleteExportedFilesAndDisableLink(UUID jobProfileId) {
    var jobExecutionsWithDeletedProfile = jobExecutionService.getAllByJobProfileId(jobProfileId);
    jobExecutionsWithDeletedProfile.forEach(
        jobExecution -> {
          jobExecution.setJobProfileId(null);
          jobExecution
              .getExportedFiles()
              .forEach(
                  exportedFile -> {
                    s3Client.remove(
                        getPathToStoredFiles(jobExecution.getId(), exportedFile.getFileName()));
                    exportedFile.setFileId(null);
                  });
        });
    jobExecutionService.saveAll(jobExecutionsWithDeletedProfile);
  }

  /**
   * Deletes error logs associated with the specified job profile ID.
   *
   * @param jobProfileId the UUID of the job profile whose associated error logs are to be deleted
   */
  private void deleteAssociatedErrors(UUID jobProfileId) {
    var deletedErrors = errorLogEntityCqlRepository.deleteByJobProfileId(jobProfileId);
    log.info(
        "Deleted {} error logs associated with job profile ID: {}", deletedErrors, jobProfileId);
  }
}
