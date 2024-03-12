package org.folio.dataexp.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.dataexp.domain.dto.ItemNoteTypes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "item-note-types")
public interface ItemNoteTypesClient {
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  ItemNoteTypes getItemNoteTypes(@RequestParam long limit);
}
