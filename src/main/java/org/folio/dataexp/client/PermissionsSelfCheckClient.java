package org.folio.dataexp.client;

import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "bulk-edit", configuration = FeignClientConfiguration.class)
public interface PermissionsSelfCheckClient {

  @GetMapping(value = "/permissions-self-check", produces = MediaType.APPLICATION_JSON_VALUE)
  List<String> getUserPermissionsForSelfCheck();
}
