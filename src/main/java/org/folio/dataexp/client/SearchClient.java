package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.IdsJob;
import org.folio.dataexp.domain.dto.IdsJobPayload;
import org.folio.dataexp.domain.dto.ResourceIds;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for submitting and retrieving search jobs and resource IDs.
 */
@FeignClient(name = "search/resources/jobs")
public interface SearchClient {

  /**
   * Submits a job to retrieve resource IDs.
   *
   * @param idsJobPayload the job payload
   * @return the submitted job
   */
  @PostMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
  IdsJob submitIdsJob(@RequestBody IdsJobPayload idsJobPayload);

  /**
   * Retrieves resource IDs for a given job.
   *
   * @param jobId the job ID
   * @return the resource IDs
   */
  @GetMapping(value = "/{jobId}/ids", produces = APPLICATION_JSON_VALUE)
  ResourceIds getResourceIds(@PathVariable String jobId);

  /**
   * Retrieves the status of a job.
   *
   * @param jobId the job ID
   * @return the job status
   */
  @GetMapping(value = "/{jobId}", produces = APPLICATION_JSON_VALUE)
  IdsJob getJobStatus(@PathVariable String jobId);
}
