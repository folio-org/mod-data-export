package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.ItemNoteTypes;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Feign client for retrieving item note types. */
@HttpExchange(url = "item-note-types")
public interface ItemNoteTypesClient {
  /**
   * Retrieves item note types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of item note types
   */
  @GetExchange(accept = APPLICATION_JSON_VALUE)
  ItemNoteTypes getItemNoteTypes(@RequestParam long limit);
}
