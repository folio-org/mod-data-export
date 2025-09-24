package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.LoanTypes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for retrieving loan types.
 */
@FeignClient(name = "loan-types")
public interface LoanTypesClient {
  /**
   * Retrieves loan types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of loan types
   */
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  LoanTypes getLoanTypes(@RequestParam long limit);
}
