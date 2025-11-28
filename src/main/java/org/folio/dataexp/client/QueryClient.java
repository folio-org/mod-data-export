package org.folio.dataexp.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.querytool.domain.dto.ContentsRequest;
import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.querytool.domain.dto.QueryIdentifier;
import org.folio.querytool.domain.dto.SubmitQuery;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for interacting with FQM query endpoints. */
@FeignClient(name = "query")
public interface QueryClient {
  /**
   * Start an asynchronous FQL query.
   *
   * @param submitQuery request object including entity type ID, fields, and query
   * @return response object including query identifier to poll for status
   */
  @PostMapping("")
  QueryIdentifier executeQuery(@RequestBody SubmitQuery submitQuery);

  /**
   * Retrieves FQM entities given a list of entity IDs.
   *
   * @param contentsRequest request object including entity type ID, fields, and input IDs
   * @return a list of entities as field-value maps
   */
  @PostMapping("/contents")
  List<Map<String, Object>> getContents(@RequestBody ContentsRequest contentsRequest);

  /**
   * Retrieve query status.
   *
   * @param queryId query identifier from executeQuery response
   * @param includeResults optionally include actual query results if available
   * @return query details object with processing status
   */
  @GetMapping("/{queryId}")
  QueryDetails getQuery(@RequestHeader UUID queryId, @RequestParam Boolean includeResults);

  /**
   * Retrieve query results assuming status is complete.
   *
   * @param queryId query identifier from executeQuery response
   * @param offset paging; starting point index of result set
   * @param limit paging; number of results to return
   * @return paged list of query results
   */
  @GetMapping("/{queryId}?includeResults=true")
  QueryDetails getQuery(
      @RequestHeader UUID queryId, @RequestParam Integer offset, @RequestParam Integer limit);

  /**
   * Retrieve the query result's set of IDs only, in sorted order.
   *
   * @param queryId query identifier from executeQuery response
   * @param offset paging; starting point index of result set
   * @param limit paging; number of results to return
   * @return paged list of sorted result identifiers
   */
  @GetMapping("/{queryId}/sortedIds")
  List<List<String>> getSortedIds(
      @RequestHeader UUID queryId, @RequestParam Integer offset, @RequestParam Integer limit);
}
