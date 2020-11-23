package org.folio.service;

import org.folio.service.logs.AffectedRecordBuilder;
import org.folio.service.logs.AffectedRecordHoldingBuilder;
import org.folio.service.logs.AffectedRecordInstanceBuilder;
import org.folio.service.logs.AffectedRecordItemBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ComponentScan(basePackages = {
  "org.folio.dao",
  "org.folio.service",
  "org.folio.clients",
  "org.folio.rest.impl"})
public class ApplicationTestConfig {

  @Bean
  @Qualifier("affectedRecordBuilders")
  public Map<String, AffectedRecordBuilder> getBuilders(@Autowired AffectedRecordInstanceBuilder affectedRecordInstanceBuilder,
                                                        @Autowired AffectedRecordHoldingBuilder affectedRecordHoldingBuilder,
                                                        @Autowired AffectedRecordItemBuilder affectedRecordItemBuilder) {
    Map<String, AffectedRecordBuilder> builders = new HashMap<>();
    builders.put(AffectedRecordInstanceBuilder.class.getName(), affectedRecordInstanceBuilder);
    builders.put(AffectedRecordHoldingBuilder.class.getName(), affectedRecordHoldingBuilder);
    builders.put(AffectedRecordItemBuilder.class.getName(), affectedRecordItemBuilder);
    return builders;
  }

}
