package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.MarcDeletedIdsCollection;
import org.folio.dataexp.rest.resource.MarcDeletedIdsApi;
import org.folio.dataexp.service.MarcDeletedIdsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

import static java.util.Objects.nonNull;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class MarcDeletedIdsController implements MarcDeletedIdsApi {

  private final MarcDeletedIdsService marcDeletedIdsService;

  @Override
  public ResponseEntity<MarcDeletedIdsCollection> getMarcDeletedIds(Date from, Date to) {
    var dateFrom = nonNull(from) ? from.toInstant() : null;
    var dateTo = nonNull(to) ? to.toInstant() : null;
    log.info("GET MARC deleted IDs with date from {}, date to {}", dateFrom, dateTo);
    var marcDeletedIdsCollection = marcDeletedIdsService.getMarcDeletedIds(from, to);
    return new ResponseEntity<>(marcDeletedIdsCollection, HttpStatus.OK);
  }
}
