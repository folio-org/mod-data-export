package org.folio.service;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
  "org.folio.dao",
  "org.folio.service",
  "org.folio.clients",
  "org.folio.rest.impl"})
public class ApplicationTestConfig {
}
