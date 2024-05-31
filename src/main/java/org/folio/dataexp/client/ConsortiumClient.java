package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.ConsortiaCollection;
import org.folio.dataexp.domain.dto.UserTenantCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "consortia")
public interface ConsortiumClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ConsortiaCollection getConsortia();

  @GetMapping(value = "/{consortiumId}/user-tenants", produces = MediaType.APPLICATION_JSON_VALUE)
  UserTenantCollection getConsortiaUserTenants(@PathVariable String consortiumId, @RequestParam String userId);
}
