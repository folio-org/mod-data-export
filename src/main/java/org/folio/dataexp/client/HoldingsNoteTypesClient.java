package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.HoldingsNoteTypes;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving holdings note types. */
@HttpExchange(url = "holdings-note-types")
public interface HoldingsNoteTypesClient {
  /**
   * Retrieves holdings note types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of holdings note types
   */
  @GetExchange(accept = APPLICATION_JSON_VALUE)
  HoldingsNoteTypes getHoldingsNoteTypes(@RequestParam long limit);
}
