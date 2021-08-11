package org.folio.config;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

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

  private static final int REQUEST_TIMEOUT_TWO_HOURS = 3600000;

  @Bean
  @Qualifier("affectedRecordBuilders")
  public Map<String, AffectedRecordBuilder> getBuilders(@Autowired AffectedRecordInstanceBuilder affectedRecordInstanceBuilder,
                                                        @Autowired AffectedRecordHoldingBuilder affectedRecordHoldingBuilder,
                                                        @Autowired AffectedRecordItemBuilder affectedRecordItemBuilder) {
    return ImmutableMap.<String, AffectedRecordBuilder>builder()
      .put(AffectedRecordInstanceBuilder.class.getName(), affectedRecordInstanceBuilder)
      .put(AffectedRecordHoldingBuilder.class.getName(), affectedRecordHoldingBuilder)
      .put(AffectedRecordItemBuilder.class.getName(), affectedRecordItemBuilder)
      .build();
  }

  @Bean
  public WebClient getWebClient(@Autowired Vertx vertx) {
    WebClientOptions webClientOptions = new WebClientOptions()
      .setKeepAliveTimeout(REQUEST_TIMEOUT_TWO_HOURS)
      .setConnectTimeout(REQUEST_TIMEOUT_TWO_HOURS);
    return WebClient.create(vertx, webClientOptions);
  }

}
