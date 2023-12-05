package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.NatureOfContentTerms;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "nature-of-content-terms")
public interface NatureOfContentTermsClient {
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  NatureOfContentTerms getNatureOfContentTerms(@RequestParam long limit);
}
