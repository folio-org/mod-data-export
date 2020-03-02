package org.folio.service;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
  "org.folio.dao",
  "org.folio.service.upload",
  "org.folio.service.loader",
  "org.folio.service.export",
  "org.folio.service.mapping",
  "org.folio.service.cleanup",
  "org.folio.service.upload.storage",
  "org.folio.clients",
  "org.folio.rest.impl"})
public class ApplicationTestConfig {
}
