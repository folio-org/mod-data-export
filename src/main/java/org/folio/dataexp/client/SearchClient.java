package org.folio.dataexp.client;

import org.folio.dataexp.domain.dto.IdsJob;
import org.folio.dataexp.domain.dto.IdsJobPayload;
import org.folio.dataexp.domain.dto.ResourceIds;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "search/resources/jobs")
public interface SearchClient {

  @PostMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
  IdsJob submitIdsJob(@RequestBody IdsJobPayload idsJobPayload);

  @GetMapping(value = "/{jobId}/ids", produces = APPLICATION_JSON_VALUE)
  ResourceIds getResourceIds(@PathVariable String jobId);
}
