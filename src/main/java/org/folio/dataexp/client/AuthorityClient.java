package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.AuthorityCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "authority-storage/authorities")
public interface AuthorityClient {

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  AuthorityCollection getAuthorities(@RequestParam boolean idOnly, @RequestParam boolean deleted, @RequestParam String query,
                                     @RequestParam long limit, @RequestParam long offset);
}
