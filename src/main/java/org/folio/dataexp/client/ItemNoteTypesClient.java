package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.ItemNoteTypes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for retrieving item note types. */
@FeignClient(name = "item-note-types")
public interface ItemNoteTypesClient {
  /**
   * Retrieves item note types with a specified limit.
   *
   * @param limit the maximum number of records to return
   * @return a collection of item note types
   */
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  ItemNoteTypes getItemNoteTypes(@RequestParam long limit);
}
