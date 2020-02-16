package org.folio.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
  "org.folio.dao",
  "org.folio.service.upload",
  "org.folio.service.loader",
  "org.folio.service.export",
  "org.folio.service.mapping",
  "org.folio.rest.impl"})
public class ApplicationConfig {
}
