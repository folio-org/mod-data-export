package org.folio.dataexp.controllers;

import static org.folio.dataexp.util.Constants.QUERY_CQL_ALL_RECORDS;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.FileDownload;
import org.folio.dataexp.domain.dto.JobExecutionCollection;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.repository.JobExecutionEntityCqlRepository;
import org.folio.dataexp.rest.resource.JobExecutionsApi;
import org.folio.dataexp.service.file.download.FileDownloadService;
import org.folio.spring.data.OffsetRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for job execution operations.
 */
@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class JobExecutionsController implements JobExecutionsApi {

  private final JobExecutionEntityCqlRepository jobExecutionEntityCqlRepository;
  private final FileDownloadService filesDownloadService;

  /**
   * Gets job executions by CQL query.
   *
   * @param query CQL query string
   * @param offset offset for pagination
   * @param limit limit for pagination
   * @return response entity with job execution collection
   */
  @Override
  public ResponseEntity<JobExecutionCollection> getJobExecutionsByQuery(
      String query,
      Integer offset,
      Integer limit
  ) {
    if (StringUtils.isEmpty(query)) {
      query = QUERY_CQL_ALL_RECORDS;
    }
    var jobExecutionsEntityPage = jobExecutionEntityCqlRepository.findByCql(
        query,
        OffsetRequest.of(offset, limit)
    );
    var jobExecutions = jobExecutionsEntityPage.stream()
        .map(JobExecutionEntity::getJobExecution)
        .toList();
    var jobExecutionCollection = new JobExecutionCollection();
    jobExecutionCollection.setJobExecutions(jobExecutions);
    jobExecutionCollection.setTotalRecords(
        (int) jobExecutionsEntityPage.getTotalElements()
    );
    return new ResponseEntity<>(
        jobExecutionCollection,
        HttpStatus.OK
    );
  }

  /**
   * Deletes a job execution by its ID.
   *
   * @param jobExecutionId job execution UUID
   * @return response entity with no content status
   */
  @Override
  public ResponseEntity<Void> deleteJobExecutionById(UUID jobExecutionId) {
    jobExecutionEntityCqlRepository.deleteById(jobExecutionId);
    return new ResponseEntity<>(
        HttpStatus.NO_CONTENT
    );
  }

  /**
   * Gets a link to download files for a job execution.
   *
   * @param jobExecutionId job execution UUID
   * @param exportFileId export file UUID
   * @return response entity with file download link
   */
  @Override
  public ResponseEntity<FileDownload> getLinkToDownloadFiles(
      UUID jobExecutionId,
      UUID exportFileId
  ) {
    var fileDownload = filesDownloadService.getFileDownload(jobExecutionId, exportFileId);
    return new ResponseEntity<>(
        fileDownload,
        HttpStatus.OK
    );
  }
}
