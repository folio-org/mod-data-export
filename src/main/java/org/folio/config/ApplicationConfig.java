package org.folio.config;

import com.google.common.collect.ImmutableMap;
import org.folio.service.logs.AffectedRecordBuilder;
import org.folio.service.logs.AffectedRecordHoldingBuilder;
import org.folio.service.logs.AffectedRecordInstanceBuilder;
import org.folio.service.logs.AffectedRecordItemBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ComponentScan(basePackages = {
  "org.folio.dao",
  "org.folio.service",
  "org.folio.clients",
  "org.folio.rest.impl"})
public class ApplicationConfig {
  @Autowired
  AffectedRecordInstanceBuilder affectedRecordInstanceBuilder;
  @Autowired
  AffectedRecordHoldingBuilder affectedRecordHoldingBuilder;
  @Autowired
  AffectedRecordItemBuilder affectedRecordItemBuilder;

  @Bean
  @Qualifier("affectedRecordBuilders")
  public Map<String, AffectedRecordBuilder> getBuilders() {
    return ImmutableMap.<String, AffectedRecordBuilder>builder()
      .put(AffectedRecordInstanceBuilder.class.getName(), affectedRecordInstanceBuilder)
      .put(AffectedRecordHoldingBuilder.class.getName(), affectedRecordHoldingBuilder)
      .put(AffectedRecordItemBuilder.class.getName(), affectedRecordItemBuilder)
      .build();
  }
}
