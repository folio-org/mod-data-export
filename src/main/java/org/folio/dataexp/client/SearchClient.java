package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.IdsJob;
import org.folio.dataexp.domain.dto.IdsJobPayload;
import org.folio.dataexp.domain.dto.ResourceIds;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/** Feign client for submitting and retrieving search jobs and resource IDs. */
@HttpExchange(url = "search/resources/jobs")
public interface SearchClient {

  /**
   * Submits a job to retrieve resource IDs.
   *
   * @param idsJobPayload the job payload
   * @return the submitted job
   */
  @PostExchange(accept = APPLICATION_JSON_VALUE)
  IdsJob submitIdsJob(@RequestBody IdsJobPayload idsJobPayload);

  /**
   * Retrieves resource IDs for a given job.
   *
   * @param jobId the job ID
   * @return the resource IDs
   */
  @GetExchange(value = "/{jobId}/ids", accept = APPLICATION_JSON_VALUE)
  ResourceIds getResourceIds(@PathVariable String jobId);

  /**
   * Retrieves the status of a job.
   *
   * @param jobId the job ID
   * @return the job status
   */
  @GetExchange(value = "/{jobId}", accept = APPLICATION_JSON_VALUE)
  IdsJob getJobStatus(@PathVariable String jobId);
}
