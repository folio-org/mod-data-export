package org.folio.dataexp.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.Config;
import org.folio.dataexp.service.ConfigurationService;
import org.folio.dataexp.rest.resource.ConfigurationApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/data-export")
public class ConfigurationController implements ConfigurationApi {

  private final ConfigurationService configurationService;

  @Override
  public ResponseEntity<Config> postDataExportConfiguration(Config config) {
    var saved = configurationService.upsertConfiguration(config);
    return new ResponseEntity<>(saved, HttpStatus.CREATED);
  }
}
