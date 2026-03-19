package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.ConsortiaCollection;
import org.folio.dataexp.domain.dto.UserTenantCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving consortia and user tenants. */
@HttpExchange(url = "consortia")
public interface ConsortiumClient {

  /**
   * Retrieves all consortia.
   *
   * @return a collection of consortia
   */
  @GetExchange(accept = MediaType.APPLICATION_JSON_VALUE)
  ConsortiaCollection getConsortia();

  /**
   * Retrieves user tenants for a specific consortium and user.
   *
   * @param consortiumId the consortium ID
   * @param userId the user ID
   * @param limit the maximum number of records to return
   * @return a collection of user tenants
   */
  @GetExchange(value = "/{consortiumId}/user-tenants", accept = MediaType.APPLICATION_JSON_VALUE)
  UserTenantCollection getConsortiaUserTenants(
      @PathVariable String consortiumId, @RequestParam String userId, @RequestParam int limit);
}
