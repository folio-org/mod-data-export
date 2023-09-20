package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "users")
public interface UserClient {

  @GetMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
  User getUserById(@PathVariable String userId);

}
