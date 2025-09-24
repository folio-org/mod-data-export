package org.folio.dataexp.controllers;

import static org.folio.dataexp.util.Constants.QUERY_CQL_ALL_RECORDS;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.ErrorLogCollection;
import org.folio.dataexp.domain.entity.ErrorLogEntity;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.rest.resource.LogsApi;
import org.folio.spring.data.OffsetRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for error log operations.
 */
@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class LogsController implements LogsApi {

  private final ErrorLogEntityCqlRepository errorLogEntityCqlRepository;

  /**
   * Gets error logs by CQL query.
   *
   * @param query CQL query string
   * @param offset offset for pagination
   * @param limit limit for pagination
   * @return response entity with error log collection
   */
  @Override
  public ResponseEntity<ErrorLogCollection> getErrorLogsByQuery(
      String query,
      Integer offset,
      Integer limit
  ) {
    if (StringUtils.isEmpty(query)) {
      query = QUERY_CQL_ALL_RECORDS;
    }
    var errorLogsPage = errorLogEntityCqlRepository.findByCql(
        query,
        OffsetRequest.of(offset, limit)
    );
    var errorLogs = errorLogsPage.stream()
        .map(ErrorLogEntity::getErrorLog)
        .toList();
    var errorLogCollection = new ErrorLogCollection()
        .errorLogs(errorLogs)
        .totalRecords((int) errorLogsPage.getTotalElements());
    return new ResponseEntity<>(
        errorLogCollection,
        HttpStatus.OK
    );
  }
}
