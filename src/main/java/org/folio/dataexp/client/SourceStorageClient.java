package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.MarcRecordIdentifiersPayload;
import org.folio.dataexp.domain.dto.MarcRecordsIdentifiersResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/** Feign client for retrieving MARC record identifiers from source storage. */
@HttpExchange(url = "source-storage/stream/marc-record-identifiers")
public interface SourceStorageClient {

  /**
   * Retrieves MARC record identifiers for the given payload.
   *
   * @param marcRecordIdentifiersPayload the request payload
   * @return the response containing MARC record identifiers
   */
  @PostExchange(accept = APPLICATION_JSON_VALUE)
  MarcRecordsIdentifiersResponse getMarcRecordsIdentifiers(
      @RequestBody MarcRecordIdentifiersPayload marcRecordIdentifiersPayload);
}
