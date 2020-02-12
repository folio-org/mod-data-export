package org.folio.service.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
  "org.folio.service.loader",
  "org.folio.rest.impl"})
public class ApplicationTestConfig {
}
