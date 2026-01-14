package org.folio.dataexp.service;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static org.folio.dataexp.util.S3FilePathUtils.getPathToStoredFiles;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.exception.job.profile.DefaultJobProfileException;
import org.folio.dataexp.exception.job.profile.LockedJobProfileException;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.s3.client.FolioS3Client;
import org.springframework.stereotype.Service;

/** Service for managing job profiles, including deletion and associated file handling. */
@Service
@RequiredArgsConstructor
@Log4j2
public class JobProfileService {

  private final JobProfileEntityRepository jobProfileEntityRepository;
  private final FolioS3Client s3Client;
  private final JobExecutionService jobExecutionService;
  private final ErrorLogEntityCqlRepository errorLogEntityCqlRepository;

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

  private void deleteAssociatedErrors(UUID jobProfileId) {
    var deletedErrors = errorLogEntityCqlRepository.deleteByJobProfileId(jobProfileId);
    log.info(
        "Deleted {} error logs associated with job profile ID: {}", deletedErrors, jobProfileId);
  }
}
