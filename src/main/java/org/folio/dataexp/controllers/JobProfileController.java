package org.folio.dataexp.controllers;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.JobProfileCollection;
import org.folio.dataexp.rest.resource.JobProfilesApi;
import org.folio.dataexp.service.JobProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for job profile operations. */
@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class JobProfileController implements JobProfilesApi {

  private final JobProfileService jobProfileService;

  /**
   * Deletes a job profile by its ID.
   *
   * @param jobProfileId job profile UUID
   * @return response entity with no content status
   */
  @Override
  public ResponseEntity<Void> deleteJobProfileById(UUID jobProfileId) {
    jobProfileService.deleteJobProfileById(jobProfileId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  /**
   * Gets a job profile by its ID.
   *
   * @param jobProfileId job profile UUID
   * @return response entity with job profile
   */
  @Override
  public ResponseEntity<JobProfile> getJobProfileById(UUID jobProfileId) {
    var jobProfile = jobProfileService.getJobProfileById(jobProfileId);
    return new ResponseEntity<>(jobProfile, HttpStatus.OK);
  }

  /**
   * Gets job profiles, optionally filtered by usage or query.
   *
   * @param used whether to filter by used profiles
   * @param query CQL query string
   * @param offset offset for pagination
   * @param limit limit for pagination
   * @return response entity with job profile collection
   */
  @Override
  public ResponseEntity<JobProfileCollection> getJobProfiles(
      Boolean used, String query, Integer offset, Integer limit) {
    return new ResponseEntity<>(
        jobProfileService.getJobProfiles(used, query, offset, limit), HttpStatus.OK);
  }

  /**
   * Creates a new job profile.
   *
   * @param jobProfile job profile object
   * @return response entity with created job profile
   */
  @Override
  public ResponseEntity<JobProfile> postJobProfile(JobProfile jobProfile) {
    return new ResponseEntity<>(jobProfileService.postJobProfile(jobProfile), HttpStatus.CREATED);
  }

  /**
   * Updates a job profile by its ID.
   *
   * @param jobProfileId job profile UUID
   * @param jobProfile job profile object
   * @return response entity with no content status
   */
  @Override
  public ResponseEntity<Void> putJobProfile(UUID jobProfileId, JobProfile jobProfile) {
    jobProfileService.putJobProfile(jobProfileId, jobProfile);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
