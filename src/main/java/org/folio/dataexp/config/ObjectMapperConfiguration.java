package org.folio.dataexp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

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
  ObjectMapper dataExportObjectMapper(Jackson2ObjectMapperBuilder builder) {
    return builder.build();
  }
}
