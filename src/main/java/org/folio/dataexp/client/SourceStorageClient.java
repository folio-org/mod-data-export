package org.folio.dataexp.client;

import feign.Headers;
import feign.Param;
import org.folio.dataexp.domain.dto.MarcRecordIdentifiersPayload;
import org.folio.dataexp.domain.dto.MarcRecordsIdentifiersResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(name = "source-storage/stream/marc-record-identifiers")
public interface SourceStorageClient {

  @PostMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
  MarcRecordsIdentifiersResponse getMarcRecordsIdentifiers(@RequestBody MarcRecordIdentifiersPayload marcRecordIdentifiersPayload);

  @PostMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
  @Headers("X-Okapi-Tenant: {tenantId}")
  MarcRecordsIdentifiersResponse getMarcRecordsIdentifiers(@RequestBody MarcRecordIdentifiersPayload marcRecordIdentifiersPayload,
                                                           @Param("tenantId") String tenantId);

}
