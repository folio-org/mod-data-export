package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.UserTenantCollection;
import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving user tenant collections in a consortium. */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
public interface ConsortiaClient {

  /**
   * Retrieves a collection of user tenants with a limit of 1.
   *
   * @return a collection of user tenants
   */
  @GetExchange(value = "user-tenants?limit=1")
  UserTenantCollection getUserTenantCollection();
}
