package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.HoldingsNoteTypes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for retrieving holdings note types.
 */
@FeignClient(name = "holdings-note-types")
public interface HoldingsNoteTypesClient {
  /**
   * Retrieves holdings note types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of holdings note types
   */
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  HoldingsNoteTypes getHoldingsNoteTypes(@RequestParam long limit);
}
