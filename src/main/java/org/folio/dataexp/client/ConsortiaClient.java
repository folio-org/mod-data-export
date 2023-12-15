package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.UserTenantCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "user-tenants")
public interface ConsortiaClient {

  @GetMapping(value = "?limit=1", produces = MediaType.APPLICATION_JSON_VALUE)
  UserTenantCollection getUserTenantCollection();
}
