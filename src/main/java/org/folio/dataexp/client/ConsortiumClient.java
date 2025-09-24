package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.ConsortiaCollection;
import org.folio.dataexp.domain.dto.UserTenantCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for retrieving consortia and user tenants.
 */
@FeignClient(name = "consortia")
public interface ConsortiumClient {

  /**
   * Retrieves all consortia.
   *
   * @return a collection of consortia
   */
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  ConsortiaCollection getConsortia();

  /**
   * Retrieves user tenants for a specific consortium and user.
   *
   * @param consortiumId the consortium ID
   * @param userId the user ID
   * @param limit the maximum number of records to return
   * @return a collection of user tenants
   */
  @GetMapping(value = "/{consortiumId}/user-tenants", produces = MediaType.APPLICATION_JSON_VALUE)
  UserTenantCollection getConsortiaUserTenants(@PathVariable String consortiumId,
      @RequestParam String userId, @RequestParam int limit);
}
