package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.AuthorityCollection;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving authority records. */
@HttpExchange(url = "authority-storage/authorities")
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
  @GetExchange(accept = APPLICATION_JSON_VALUE)
  AuthorityCollection getAuthorities(
      @RequestParam boolean idOnly,
      @RequestParam boolean deleted,
      @RequestParam(required = false) String query,
      @RequestParam long limit,
      @RequestParam long offset);
}
