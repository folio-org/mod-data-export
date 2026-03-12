package org.folio.dataexp.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Configuration class for JSON object mapping. */
@Configuration
public class ObjectMapperConfiguration {
  /**
   * Configures an object mapper for reuse in JSON serialization and deserialization.
   *
   * @return a configured object mapper
   */
  @Bean
  ObjectMapper dataExportObjectMapper() {
    return JsonMapper.builder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        .changeDefaultPropertyInclusion(
            incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
        .changeDefaultPropertyInclusion(
            incl -> incl.withContentInclusion(JsonInclude.Include.NON_NULL))
        .build();
  }
}
