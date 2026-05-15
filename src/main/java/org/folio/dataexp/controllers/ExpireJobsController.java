package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.rest.resource.ExpireJobsApi;
import org.folio.dataexp.service.JobExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for expiring job executions. */
@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class ExpireJobsController implements ExpireJobsApi {
  private final JobExecutionService jobExecutionService;

  /**
   * Expires job executions.
   *
   * @return response entity with no content status
   */
  @Override
  public ResponseEntity<Void> postExpireJobExecution() {
    jobExecutionService.expireJobExecutions();
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
