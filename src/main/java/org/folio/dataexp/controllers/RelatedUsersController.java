package org.folio.dataexp.controllers;

import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.RelatedUser;
import org.folio.dataexp.domain.dto.RelatedUserCollection;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.repository.JobExecutionEntityCqlRepository;
import org.folio.dataexp.rest.resource.RelatedUsersApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for related users operations.
 */
@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class RelatedUsersController implements RelatedUsersApi {

  private final JobExecutionEntityCqlRepository jobExecutionEntityCqlRepository;

  /**
   * Gets related users from job executions.
   *
   * @return response entity with related user collection
   */
  @Override
  public ResponseEntity<RelatedUserCollection> getRelatedUsers() {
    log.info("GET related users");
    var relatedUsers = jobExecutionEntityCqlRepository.findAll().stream()
        .map(JobExecutionEntity::getJobExecution)
        .filter(job -> nonNull(job.getRunBy()))
        .map(JobExecution::getRunBy)
        .map(runBy -> new RelatedUser()
            .userId(runBy.getUserId())
            .firstName(runBy.getFirstName())
            .lastName(runBy.getLastName())
        )
        .collect(Collectors.toSet());
    log.info("Related users size: {}", relatedUsers.size());
    var relatedUserCollection = new RelatedUserCollection()
        .relatedUsers(new ArrayList<>(relatedUsers))
        .totalRecords(relatedUsers.size());
    return new ResponseEntity<>(
        relatedUserCollection,
        HttpStatus.OK
    );
  }
}
