package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.UserTenantCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

/** Feign client for retrieving user tenant collections in a consortium. */
@FeignClient(name = "user-tenants")
public interface ConsortiaClient {

  /**
   * Retrieves a collection of user tenants with a limit of 1.
   *
   * @return a collection of user tenants
   */
  @GetMapping(value = "?limit=1", produces = MediaType.APPLICATION_JSON_VALUE)
  UserTenantCollection getUserTenantCollection();
}
