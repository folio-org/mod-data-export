package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import org.folio.dataexp.domain.dto.MarcRecordIdentifiersPayload;
import org.folio.dataexp.domain.dto.MarcRecordsIdentifiersResponse;
import org.folio.dataexp.domain.dto.srsresponse.MarcRecordsResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/** Feign client for retrieving MARC record identifiers from source storage. */
@HttpExchange(url = "source-storage")
public interface SourceStorageClient {

  /**
   * Retrieves MARC record identifiers for the given payload.
   *
   * @param marcRecordIdentifiersPayload the request payload
   * @return the response containing MARC record identifiers
   */
  @PostExchange(value = "/stream/marc-record-identifiers",
      accept = APPLICATION_JSON_VALUE)
  MarcRecordsIdentifiersResponse getMarcRecordsIdentifiers(
      @RequestBody MarcRecordIdentifiersPayload marcRecordIdentifiersPayload);

  /**
   * Retrieves MARC records by external IDs.
   *
   * @param externalIds the list of external IDs for which to retrieve MARC records
   * @return the response containing MARC records
   */
  @PostExchange(value = "/source-records?idType=INSTANCE",
      accept = APPLICATION_JSON_VALUE)
  MarcRecordsResponse getMarcRecordsByExternalIds(
      @RequestBody List<String> externalIds);
}
