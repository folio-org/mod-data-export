package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.AuthorityCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for retrieving authority records.
 */
@FeignClient(name = "authority-storage/authorities")
public interface AuthorityClient {

  /**
   * Retrieves a collection of authorities based on query parameters.
   *
   * @param idOnly whether to return only IDs
   * @param deleted whether to include deleted records
   * @param query the CQL query string
   * @param limit the maximum number of records to return
   * @param offset the starting offset
   * @return a collection of authorities
   */
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  AuthorityCollection getAuthorities(@RequestParam boolean idOnly, @RequestParam boolean deleted,
      @RequestParam String query, @RequestParam long limit, @RequestParam long offset);
}
