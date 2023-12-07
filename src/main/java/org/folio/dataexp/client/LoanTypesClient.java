package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.LoanTypes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "loan-types")
public interface LoanTypesClient {
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  LoanTypes getLoanTypes(@RequestParam long limit);
}
