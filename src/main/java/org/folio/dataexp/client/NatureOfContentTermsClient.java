package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.NatureOfContentTerms;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for retrieving nature of content terms.
 */
@FeignClient(name = "nature-of-content-terms")
public interface NatureOfContentTermsClient {
  /**
   * Retrieves nature of content terms with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of nature of content terms
   */
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  NatureOfContentTerms getNatureOfContentTerms(@RequestParam long limit);
}
