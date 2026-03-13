package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.LoanTypes;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving loan types. */
@HttpExchange(url = "loan-types")
public interface LoanTypesClient {
  /**
   * Retrieves loan types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of loan types
   */
  @GetExchange(accept = APPLICATION_JSON_VALUE)
  LoanTypes getLoanTypes(@RequestParam long limit);
}
