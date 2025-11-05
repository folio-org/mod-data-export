package org.folio.dataexp.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration class for JSON object mapping.
 */
@Configuration
public class ObjectMapperConfiguration {
  /**
   * Configures an object mapper for reuse in JSON serialization and deserialization.
   *
   * @return a configured object mapper
   */
  @Bean
  @Primary
  public ObjectMapper dataExportObjectMapper() {
    return new ObjectMapper()
      .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
  }
}
