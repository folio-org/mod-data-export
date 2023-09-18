package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.JobExecutionCollection;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.repository.JobExecutionEntityCqlRepository;
import org.folio.dataexp.rest.resource.JobExecutionsApi;
import org.folio.spring.data.OffsetRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class JobExecutionsController implements JobExecutionsApi {

  private final JobExecutionEntityCqlRepository jobExecutionEntityCqlRepository;

  @Override
  public ResponseEntity<JobExecutionCollection> getJobExecutionsByQuery(String query, Integer offset, Integer limit) {
    if (StringUtils.isEmpty(query)) query = "(cql.allRecords=1)";
    var jobExecutionsEntityPage  = jobExecutionEntityCqlRepository.findByCQL(query, OffsetRequest.of(offset, limit));
    var jobExecutions = jobExecutionsEntityPage.toList().stream().map(JobExecutionEntity::getJobExecution).toList();
    var jobExecutionCollection = new JobExecutionCollection();
    jobExecutionCollection.setJobExecutions(jobExecutions);
    jobExecutionCollection.setTotalRecords((int) jobExecutionsEntityPage.getTotalElements());
    return new ResponseEntity<>(jobExecutionCollection, HttpStatus.OK);
  }
}
