package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.UserTenantCollection;
import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving user tenant collections in a consortium. */
@HttpExchange(url = "user-tenants")
public interface ConsortiaClient {

  /**
   * Retrieves a collection of user tenants with a limit of 1.
   *
   * @return a collection of user tenants
   */
  @GetExchange(value = "?limit=1", accept = MediaType.APPLICATION_JSON_VALUE)
  UserTenantCollection getUserTenantCollection();
}
